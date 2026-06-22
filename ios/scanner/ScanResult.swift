import Foundation

struct ScanResult {
  let value: String
  let format: String
  let timestamp: Int64

  func toDictionary() -> [String: Any] {
    [
      "value": value,
      "format": format,
      "timestamp": timestamp,
    ]
  }
}
