package expo.modules.logisticsscanner

import expo.modules.kotlin.modules.Module
import expo.modules.kotlin.modules.ModuleDefinition
import expo.modules.logisticsscanner.scanner.ScannerMetrics
import expo.modules.logisticsscanner.scanner.ScannerView

class ExpoLogisticsScannerModule : Module() {
  override fun definition() = ModuleDefinition {
    Name("ExpoLogisticsScanner")

    AsyncFunction("getScannerMetrics") {
      ScannerMetrics.toMap()
    }

    View(ScannerView::class) {
      Events("onScan", "onError")

      Prop("torch") { view: ScannerView, enabled: Boolean ->
        view.setTorchEnabled(enabled)
      }

      Prop("duplicateTimeout") { view: ScannerView, timeoutMs: Int ->
        view.setDuplicateTimeout(timeoutMs)
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
