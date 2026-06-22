import Foundation

protocol ScannerProvider: AnyObject {
  func start(
    onScan: @escaping (ScanResult) -> Void,
    onStarted: @escaping () -> Void,
    onFailed: @escaping () -> Void
  )
  func stop()
  func setTorch(enabled: Bool)
}
