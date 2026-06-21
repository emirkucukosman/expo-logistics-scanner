package expo.modules.logisticsscanner

import expo.modules.kotlin.modules.Module
import expo.modules.kotlin.modules.ModuleDefinition
import expo.modules.logisticsscanner.scanner.ScannerView

class ExpoLogisticsScannerModule : Module() {
  override fun definition() = ModuleDefinition {
    Name("ExpoLogisticsScanner")

    View(ScannerView::class) {
      Events("onScan")

      Prop("torch") { view: ScannerView, enabled: Boolean ->
        view.setTorchEnabled(enabled)
      }

      OnViewDidUpdateProps { view ->
        view.startScanning()
      }

      OnViewDestroys { view ->
        view.stopScanning()
      }
    }
  }
}
