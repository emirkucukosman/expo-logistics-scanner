import Foundation
import os.log
import UIKit

enum ScannerMetrics {
  private static let lock = NSLock()
  private static var startRequestTimeMs: Int64 = 0

  private(set) static var cameraStartupMs: Int64 = 0
  private(set) static var lastDecodeLatencyMs: Int64 = 0
  private(set) static var scanCount: Int64 = 0

  static func markStartRequested() {
    lock.lock()
    startRequestTimeMs = Int64(Date().timeIntervalSince1970 * 1000)
    lock.unlock()
  }

  static func markCameraStarted() {
    lock.lock()
    defer { lock.unlock() }

    guard startRequestTimeMs > 0 else {
      return
    }

    let now = Int64(Date().timeIntervalSince1970 * 1000)
    cameraStartupMs = now - startRequestTimeMs
    startRequestTimeMs = 0
    logIfDebug()
  }

  static func recordDecodeLatency(_ latencyMs: Int64) {
    lock.lock()
    lastDecodeLatencyMs = latencyMs
    lock.unlock()
  }

  static func incrementScanCount() {
    lock.lock()
    scanCount += 1
    lock.unlock()
    logIfDebug()
  }

  static func toDictionary() -> [String: Any] {
    lock.lock()
    defer { lock.unlock() }

    return [
      "cameraStartupMs": cameraStartupMs,
      "lastDecodeLatencyMs": lastDecodeLatencyMs,
      "scanCount": scanCount,
    ]
  }

  private static func logIfDebug() {
    #if DEBUG
    os_log(
      "ScannerMetrics: startup=%{public}lldms decode=%{public}lldms scans=%{public}lld",
      log: .default,
      type: .debug,
      cameraStartupMs,
      lastDecodeLatencyMs,
      scanCount
    )
    #endif
  }
}

final class ScannerManager {
  private let previewView: UIView
  private let provider: CameraScannerProvider

  init(previewView: UIView) {
    self.previewView = previewView
    self.provider = CameraScannerProvider(previewView: previewView)
  }

  func start(
    onScan: @escaping (ScanResult) -> Void,
    onStarted: @escaping () -> Void = {},
    onFailed: @escaping (ScanError) -> Void = { _ in },
    onError: @escaping (ScanError) -> Void = { _ in }
  ) {
    provider.start(onScan: onScan, onStarted: onStarted, onFailed: onFailed, onError: onError)
  }

  func stop() {
    provider.stop()
  }

  func setTorch(enabled: Bool) {
    provider.setTorch(enabled: enabled)
  }

  func updatePreviewFrame(_ frame: CGRect) {
    provider.updatePreviewFrame(frame)
  }
}
