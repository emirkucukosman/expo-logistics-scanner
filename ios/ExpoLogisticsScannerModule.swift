import ExpoModulesCore

public class ExpoLogisticsScannerModule: Module {
  public func definition() -> ModuleDefinition {
    Name("ExpoLogisticsScanner")

    AsyncFunction("getScannerMetrics") { () -> [String: Any] in
      ScannerMetrics.toDictionary()
    }

    View(ScannerView.self) {
      Events("onScan", "onError")

      Prop("torch") { (view: ScannerView, enabled: Bool) in
        view.setTorchEnabled(enabled)
      }

      Prop("duplicateTimeout") { (view: ScannerView, timeoutMs: Int) in
        view.setDuplicateTimeout(timeoutMs)
      }

      OnViewDidUpdateProps { view in
        view.startScanning()
      }
    }
  }
}
