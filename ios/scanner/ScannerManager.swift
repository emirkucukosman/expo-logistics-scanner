import UIKit

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
    onFailed: @escaping () -> Void = {}
  ) {
    provider.start(onScan: onScan, onStarted: onStarted, onFailed: onFailed)
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
