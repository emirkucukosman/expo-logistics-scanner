import AVFoundation
import Foundation
import Vision

private let code128Format = "CODE_128"

final class BarcodeAnalyzer: NSObject, AVCaptureVideoDataOutputSampleBufferDelegate {
  private let onScan: (ScanResult) -> Void
  private let analysisQueue = DispatchQueue(label: "expo.logistics.scanner.barcode-analysis")
  private var isProcessing = false
  private let processingLock = NSLock()

  init(onScan: @escaping (ScanResult) -> Void) {
    self.onScan = onScan
  }

  func makeVideoOutput() -> AVCaptureVideoDataOutput {
    let output = AVCaptureVideoDataOutput()
    output.videoSettings = [
      kCVPixelBufferPixelFormatTypeKey as String: kCVPixelFormatType_32BGRA,
    ]
    output.alwaysDiscardsLateVideoFrames = true
    output.setSampleBufferDelegate(self, queue: analysisQueue)
    return output
  }

  func captureOutput(
    _ output: AVCaptureOutput,
    didOutput sampleBuffer: CMSampleBuffer,
    from connection: AVCaptureConnection
  ) {
    processingLock.lock()
    if isProcessing {
      processingLock.unlock()
      return
    }
    isProcessing = true
    processingLock.unlock()

    defer {
      processingLock.lock()
      isProcessing = false
      processingLock.unlock()
    }

    let request = VNDetectBarcodesRequest()
    request.symbologies = [.code128]

    let orientation = cgImageOrientation(for: connection)
    let handler = VNImageRequestHandler(
      cmSampleBuffer: sampleBuffer,
      orientation: orientation,
      options: [:]
    )

    do {
      try handler.perform([request])
    } catch {
      return
    }

    guard let observation = (request.results as? [VNBarcodeObservation])?.first,
          let payload = observation.payloadStringValue
    else {
      return
    }

    let result = ScanResult(
      value: payload,
      format: code128Format,
      timestamp: Int64(Date().timeIntervalSince1970 * 1000)
    )

    DispatchQueue.main.async {
      self.onScan(result)
    }
  }

  private func cgImageOrientation(for connection: AVCaptureConnection) -> CGImagePropertyOrientation {
    switch connection.videoOrientation {
    case .portrait:
      return .right
    case .portraitUpsideDown:
      return .left
    case .landscapeRight:
      return .down
    case .landscapeLeft:
      return .up
    @unknown default:
      return .right
    }
  }
}
