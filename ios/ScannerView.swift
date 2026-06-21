import ExpoModulesCore
import UIKit

class ScannerView: ExpoView {
  let onScan = EventDispatcher()

  required init(appContext: AppContext? = nil) {
    super.init(appContext: appContext)
    clipsToBounds = true
    backgroundColor = .black
  }

  func setTorchEnabled(_ enabled: Bool) {
    // iOS scanning is not implemented in MVP.
  }
}
