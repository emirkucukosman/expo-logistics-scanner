import ExpoModulesCore
import UIKit

class ScannerView: ExpoView {
  let onScan = EventDispatcher()
  let onError = EventDispatcher()

  private let previewContainer = UIView()
  private var scannerManager: ScannerManager?
  private var isScanning = false
  private var isStarting = false
  private var torchEnabled = false
  private var duplicateTimeoutMs: Int64 = 0
  private var lastScanValue: String?
  private var lastScanEmitTimeMs: Int64 = 0

  deinit {
    scannerManager?.stop()
  }

  required init(appContext: AppContext? = nil) {
    super.init(appContext: appContext)
    clipsToBounds = true
    backgroundColor = .black

    previewContainer.backgroundColor = .black
    previewContainer.translatesAutoresizingMaskIntoConstraints = false
    addSubview(previewContainer)

    NSLayoutConstraint.activate([
      previewContainer.topAnchor.constraint(equalTo: topAnchor),
      previewContainer.leadingAnchor.constraint(equalTo: leadingAnchor),
      previewContainer.trailingAnchor.constraint(equalTo: trailingAnchor),
      previewContainer.bottomAnchor.constraint(equalTo: bottomAnchor),
    ])
  }

  func startScanning() {
    DispatchQueue.main.async { [weak self] in
      self?.tryStartScanning()
    }
  }

  func stopScanning() {
    guard isScanning || isStarting else {
      return
    }

    scannerManager?.stop()
    isScanning = false
    isStarting = false
  }

  func setTorchEnabled(_ enabled: Bool) {
    torchEnabled = enabled
    scannerManager?.setTorch(enabled: enabled)
  }

  func setDuplicateTimeout(_ timeoutMs: Int) {
    duplicateTimeoutMs = Int64(max(timeoutMs, 0))
  }

  override func layoutSubviews() {
    super.layoutSubviews()
    scannerManager?.updatePreviewFrame(previewContainer.bounds)

    if bounds.width > 0, bounds.height > 0 {
      tryStartScanning()
    }
  }

  override func didMoveToWindow() {
    super.didMoveToWindow()

    if window != nil {
      tryStartScanning()
    } else {
      stopScanning()
    }
  }

  private func tryStartScanning() {
    guard !isScanning, !isStarting, window != nil, bounds.width > 0, bounds.height > 0 else {
      return
    }

    if scannerManager == nil {
      scannerManager = ScannerManager(previewView: previewContainer)
    }

    guard let manager = scannerManager else {
      return
    }

    isStarting = true

    manager.start(
      onScan: { [weak self] result in
        self?.handleScan(result)
      },
      onStarted: { [weak self] in
        guard let self else {
          return
        }
        self.isStarting = false
        self.isScanning = true
        self.scannerManager?.setTorch(enabled: self.torchEnabled)
      },
      onFailed: { [weak self] error in
        self?.dispatchFatalError(error)
      },
      onError: { [weak self] error in
        self?.dispatchRuntimeError(error)
      }
    )
  }

  private func handleScan(_ result: ScanResult) {
    let now = Int64(Date().timeIntervalSince1970 * 1000)
    if duplicateTimeoutMs > 0,
       result.value == lastScanValue,
       now - lastScanEmitTimeMs < duplicateTimeoutMs
    {
      return
    }

    lastScanValue = result.value
    lastScanEmitTimeMs = now
    ScannerMetrics.incrementScanCount()
    onScan(result.toDictionary())
  }

  private func dispatchFatalError(_ error: ScanError) {
    isStarting = false
    isScanning = false
    onError(error.toDictionary())
  }

  private func dispatchRuntimeError(_ error: ScanError) {
    onError(error.toDictionary())
  }
}
