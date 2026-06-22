import AVFoundation
import Foundation
import UIKit

final class CameraScannerProvider: NSObject, ScannerProvider {
  private let previewView: UIView
  private let sessionQueue = DispatchQueue(label: "expo.logistics.scanner.camera-session")
  private let previewLayer = AVCaptureVideoPreviewLayer()
  private var captureSession: AVCaptureSession?
  private var captureDevice: AVCaptureDevice?
  private var barcodeAnalyzer: BarcodeAnalyzer?
  private var torchEnabled = false
  private var onScanCallback: ((ScanResult) -> Void)?
  private var onStartedCallback: (() -> Void)?
  private var onFailedCallback: (() -> Void)?

  init(previewView: UIView) {
    self.previewView = previewView
    super.init()

    previewLayer.videoGravity = .resizeAspectFill
    previewView.layer.insertSublayer(previewLayer, at: 0)
  }

  func start(
    onScan: @escaping (ScanResult) -> Void,
    onStarted: @escaping () -> Void,
    onFailed: @escaping () -> Void
  ) {
    onScanCallback = onScan
    onStartedCallback = onStarted
    onFailedCallback = onFailed

    requestCameraAccess { [weak self] granted in
      guard let self else {
        return
      }

      if !granted {
        DispatchQueue.main.async {
          onFailed()
        }
        return
      }

      self.sessionQueue.async {
        self.configureAndStartSession()
      }
    }
  }

  func stop() {
    sessionQueue.async { [weak self] in
      guard let self else {
        return
      }

      if let session = self.captureSession, session.isRunning {
        session.stopRunning()
      }

      self.captureSession = nil
      self.captureDevice = nil
      self.barcodeAnalyzer = nil
      self.onScanCallback = nil
      self.onStartedCallback = nil
      self.onFailedCallback = nil
    }
  }

  func setTorch(enabled: Bool) {
    torchEnabled = enabled
    applyTorch()
  }

  func updatePreviewFrame(_ frame: CGRect) {
    DispatchQueue.main.async {
      self.previewLayer.frame = frame
    }
  }

  private func requestCameraAccess(completion: @escaping (Bool) -> Void) {
    switch AVCaptureDevice.authorizationStatus(for: .video) {
    case .authorized:
      completion(true)
    case .notDetermined:
      AVCaptureDevice.requestAccess(for: .video, completionHandler: completion)
    case .denied, .restricted:
      completion(false)
    @unknown default:
      completion(false)
    }
  }

  private func configureAndStartSession() {
    let session = AVCaptureSession()
    session.sessionPreset = .hd1280x720

    guard let device = AVCaptureDevice.default(.builtInWideAngleCamera, for: .video, position: .back),
          let input = try? AVCaptureDeviceInput(device: device)
    else {
      DispatchQueue.main.async { [weak self] in
        self?.onFailedCallback?()
      }
      return
    }

    guard session.canAddInput(input) else {
      DispatchQueue.main.async { [weak self] in
        self?.onFailedCallback?()
      }
      return
    }
    session.addInput(input)

    guard let onScan = onScanCallback else {
      return
    }

    let analyzer = BarcodeAnalyzer(onScan: onScan)
    let videoOutput = analyzer.makeVideoOutput()

    guard session.canAddOutput(videoOutput) else {
      DispatchQueue.main.async { [weak self] in
        self?.onFailedCallback?()
      }
      return
    }
    session.addOutput(videoOutput)

    if let connection = videoOutput.connection(with: .video), connection.isVideoOrientationSupported {
      connection.videoOrientation = currentVideoOrientation()
    }

    captureSession = session
    captureDevice = device
    barcodeAnalyzer = analyzer

    DispatchQueue.main.async { [weak self] in
      guard let self else {
        return
      }

      self.previewLayer.session = session
      self.applyTorch()
    }

    session.startRunning()

    DispatchQueue.main.async { [weak self] in
      self?.onStartedCallback?()
    }
  }

  private func applyTorch() {
    sessionQueue.async { [weak self] in
      guard let self,
            let device = self.captureDevice,
            device.hasTorch
      else {
        return
      }

      do {
        try device.lockForConfiguration()
        if self.torchEnabled {
          try device.setTorchModeOn(level: AVCaptureDevice.maxAvailableTorchLevel)
        } else {
          device.torchMode = .off
        }
        device.unlockForConfiguration()
      } catch {
        // Torch failures should not crash scanning.
      }
    }
  }

  private func currentVideoOrientation() -> AVCaptureVideoOrientation {
    guard let windowScene = previewView.window?.windowScene else {
      return .portrait
    }

    switch windowScene.interfaceOrientation {
    case .portrait:
      return .portrait
    case .portraitUpsideDown:
      return .portraitUpsideDown
    case .landscapeLeft:
      return .landscapeLeft
    case .landscapeRight:
      return .landscapeRight
    default:
      return .portrait
    }
  }
}
