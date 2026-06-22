import Foundation

protocol ScannerProvider: AnyObject {
  func start(
    onScan: @escaping (ScanResult) -> Void,
    onStarted: @escaping () -> Void,
    onFailed: @escaping (ScanError) -> Void,
    onError: @escaping (ScanError) -> Void
  )
  func stop()
  func setTorch(enabled: Bool)
}
