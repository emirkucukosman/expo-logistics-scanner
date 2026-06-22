import ExpoModulesCore

public class ExpoLogisticsScannerModule: Module {
  public func definition() -> ModuleDefinition {
    Name("ExpoLogisticsScanner")

    View(ScannerView.self) {
      Events("onScan")

      Prop("torch") { (view: ScannerView, enabled: Bool) in
        view.setTorchEnabled(enabled)
      }

      OnViewDidUpdateProps { view in
        view.startScanning()
      }
    }
  }
}
