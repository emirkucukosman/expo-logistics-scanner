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
  private var shouldBeRunning = false
  private var isPausedByBackground = false
  private var lifecycleObserversRegistered = false

  private var onScanCallback: ((ScanResult) -> Void)?
  private var onStartedCallback: (() -> Void)?
  private var onFailedCallback: ((ScanError) -> Void)?
  private var onErrorCallback: ((ScanError) -> Void)?

  init(previewView: UIView) {
    self.previewView = previewView
    super.init()

    previewLayer.videoGravity = .resizeAspectFill
    previewView.layer.insertSublayer(previewLayer, at: 0)
  }

  func start(
    onScan: @escaping (ScanResult) -> Void,
    onStarted: @escaping () -> Void,
    onFailed: @escaping (ScanError) -> Void,
    onError: @escaping (ScanError) -> Void
  ) {
    onScanCallback = onScan
    onStartedCallback = onStarted
    onFailedCallback = onFailed
    onErrorCallback = onError
    shouldBeRunning = true
    isPausedByBackground = false
    registerLifecycleObservers()
    ScannerMetrics.markStartRequested()

    requestCameraAccess { [weak self] granted in
      guard let self else {
        return
      }

      if !granted {
        DispatchQueue.main.async {
          onFailed(
            ScanError(
              code: ScanError.permissionDenied,
              message: "Camera permission not granted"
            )
          )
        }
        return
      }

      self.sessionQueue.async {
        self.configureAndStartSession()
      }
    }
  }

  func stop() {
    shouldBeRunning = false
    isPausedByBackground = false
    unregisterLifecycleObservers()

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
      self.onErrorCallback = nil
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
    guard shouldBeRunning, !isPausedByBackground else {
      return
    }

    let session = AVCaptureSession()
    session.sessionPreset = .hd1280x720

    guard let device = AVCaptureDevice.default(.builtInWideAngleCamera, for: .video, position: .back),
          let input = try? AVCaptureDeviceInput(device: device)
    else {
      DispatchQueue.main.async { [weak self] in
        self?.onFailedCallback?(
          ScanError(
            code: ScanError.cameraUnavailable,
            message: "Camera device unavailable"
          )
        )
      }
      return
    }

    guard session.canAddInput(input) else {
      DispatchQueue.main.async { [weak self] in
        self?.onFailedCallback?(
          ScanError(
            code: ScanError.cameraUnavailable,
            message: "Unable to add camera input"
          )
        )
      }
      return
    }
    session.addInput(input)

    guard let onScan = onScanCallback else {
      return
    }

    let errorCallback = onErrorCallback ?? { _ in }
    let analyzer = BarcodeAnalyzer(onScan: onScan, onFailure: errorCallback)
    let videoOutput = analyzer.makeVideoOutput()

    guard session.canAddOutput(videoOutput) else {
      DispatchQueue.main.async { [weak self] in
        self?.onFailedCallback?(
          ScanError(
            code: ScanError.cameraUnavailable,
            message: "Unable to add video output"
          )
        )
      }
      return
    }
    session.addOutput(videoOutput)

    if let connection = videoOutput.connection(with: .video), connection.isVideoOrientationSupported {
      connection.videoOrientation = currentVideoOrientation()
    }

    registerSessionObservers(for: session)

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
    ScannerMetrics.markCameraStarted()

    DispatchQueue.main.async { [weak self] in
      self?.onStartedCallback?()
    }
  }

  private func pauseForBackground() {
    guard shouldBeRunning, !isPausedByBackground else {
      return
    }

    isPausedByBackground = true
    sessionQueue.async { [weak self] in
      guard let self, let session = self.captureSession, session.isRunning else {
        return
      }
      session.stopRunning()
    }
  }

  private func resumeFromBackground() {
    guard shouldBeRunning, isPausedByBackground else {
      return
    }

    isPausedByBackground = false
    sessionQueue.async { [weak self] in
      guard let self else {
        return
      }

      if let session = self.captureSession, !session.isRunning {
        session.startRunning()
        return
      }

      self.configureAndStartSession()
    }
  }

  private func registerLifecycleObservers() {
    guard !lifecycleObserversRegistered else {
      return
    }

    lifecycleObserversRegistered = true
    NotificationCenter.default.addObserver(
      self,
      selector: #selector(handleWillResignActive),
      name: UIApplication.willResignActiveNotification,
      object: nil
    )
    NotificationCenter.default.addObserver(
      self,
      selector: #selector(handleDidBecomeActive),
      name: UIApplication.didBecomeActiveNotification,
      object: nil
    )
  }

  private func unregisterLifecycleObservers() {
    guard lifecycleObserversRegistered else {
      return
    }

    lifecycleObserversRegistered = false
    NotificationCenter.default.removeObserver(self, name: UIApplication.willResignActiveNotification, object: nil)
    NotificationCenter.default.removeObserver(self, name: UIApplication.didBecomeActiveNotification, object: nil)

    if let session = captureSession {
      unregisterSessionObservers(for: session)
    }
  }

  private func registerSessionObservers(for session: AVCaptureSession) {
    NotificationCenter.default.addObserver(
      self,
      selector: #selector(handleSessionWasInterrupted),
      name: .AVCaptureSessionWasInterrupted,
      object: session
    )
    NotificationCenter.default.addObserver(
      self,
      selector: #selector(handleSessionInterruptionEnded),
      name: .AVCaptureSessionInterruptionEnded,
      object: session
    )
    NotificationCenter.default.addObserver(
      self,
      selector: #selector(handleSessionRuntimeError),
      name: .AVCaptureSessionRuntimeError,
      object: session
    )
  }

  private func unregisterSessionObservers(for session: AVCaptureSession) {
    NotificationCenter.default.removeObserver(self, name: .AVCaptureSessionWasInterrupted, object: session)
    NotificationCenter.default.removeObserver(self, name: .AVCaptureSessionInterruptionEnded, object: session)
    NotificationCenter.default.removeObserver(self, name: .AVCaptureSessionRuntimeError, object: session)
  }

  @objc private func handleWillResignActive() {
    pauseForBackground()
  }

  @objc private func handleDidBecomeActive() {
    resumeFromBackground()
  }

  @objc private func handleSessionWasInterrupted(_ notification: Notification) {
    let reason = (notification.userInfo?[AVCaptureSessionInterruptionReasonKey] as? NSNumber)
      .flatMap { AVCaptureSession.InterruptionReason(rawValue: $0.intValue) }

    let message: String
    switch reason {
    case .videoDeviceNotAvailableInBackground:
      message = "Camera unavailable while app is in background"
    case .audioDeviceInUseByAnotherClient, .videoDeviceInUseByAnotherClient:
      message = "Camera in use by another application"
    case .videoDeviceNotAvailableWithMultipleForegroundApps:
      message = "Camera unavailable with multiple foreground apps"
    case .videoDeviceNotAvailableDueToSystemPressure:
      message = "Camera unavailable due to system pressure"
    default:
      message = "Camera session interrupted"
    }

    DispatchQueue.main.async { [weak self] in
      self?.onErrorCallback?(
        ScanError(code: ScanError.interrupted, message: message)
      )
    }
  }

  @objc private func handleSessionInterruptionEnded(_ notification: Notification) {
    guard shouldBeRunning, !isPausedByBackground else {
      return
    }

    guard let session = notification.object as? AVCaptureSession else {
      return
    }

    sessionQueue.async {
      if !session.isRunning {
        session.startRunning()
      }
    }
  }

  @objc private func handleSessionRuntimeError(_ notification: Notification) {
    let underlyingError = notification.userInfo?[AVCaptureSessionErrorKey] as? NSError
    DispatchQueue.main.async { [weak self] in
      self?.onErrorCallback?(
        ScanError(
          code: ScanError.interrupted,
          message: underlyingError?.localizedDescription ?? "Camera runtime error"
        )
      )
    }

    guard shouldBeRunning, !isPausedByBackground else {
      return
    }

    sessionQueue.async { [weak self] in
      guard let self, let session = self.captureSession else {
        return
      }

      if session.canSetSessionPreset(.hd1280x720) {
        session.sessionPreset = .hd1280x720
      }

      if !session.isRunning {
        session.startRunning()
      }
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
