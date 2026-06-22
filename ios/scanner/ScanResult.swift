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

struct ScanError {
  let code: String
  let message: String

  func toDictionary() -> [String: String] {
    [
      "code": code,
      "message": message,
    ]
  }

  static let permissionDenied = "permission_denied"
  static let cameraUnavailable = "camera_unavailable"
  static let decoderFailure = "decoder_failure"
  static let interrupted = "interrupted"
}
