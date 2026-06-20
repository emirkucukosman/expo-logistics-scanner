import ExpoModulesCore

public class ExpoLogisticsScannerModule: Module {
  public func definition() -> ModuleDefinition {
    Name("ExpoLogisticsScanner")

    Events("onChange")

    Constant("PI") {
      Double.pi
    }

    Function("hello") {
      return "Hello world! 👋"
    }

    AsyncFunction("setValueAsync") { (value: String) in
      self.sendEvent("onChange", [
        "value": value
      ])
    }
  }
}
