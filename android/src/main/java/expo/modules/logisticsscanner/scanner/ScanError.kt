package expo.modules.logisticsscanner.scanner

data class ScanError(
  val code: String,
  val message: String,
) {
  fun toMap(): Map<String, String> = mapOf(
    "code" to code,
    "message" to message,
  )

  companion object {
    const val PERMISSION_DENIED = "permission_denied"
    const val CAMERA_UNAVAILABLE = "camera_unavailable"
    const val DECODER_FAILURE = "decoder_failure"
    const val INTERRUPTED = "interrupted"
  }
}
