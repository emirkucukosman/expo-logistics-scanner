package expo.modules.logisticsscanner.scanner

import android.util.Log
import expo.modules.logisticsscanner.BuildConfig

object ScannerMetrics {
  @Volatile
  var cameraStartupMs: Long = 0
    private set

  @Volatile
  var lastDecodeLatencyMs: Long = 0
    private set

  @Volatile
  var scanCount: Long = 0
    private set

  @Volatile
  private var startRequestTimeMs: Long = 0

  fun markStartRequested() {
    startRequestTimeMs = System.currentTimeMillis()
  }

  fun markCameraStarted() {
    if (startRequestTimeMs > 0) {
      cameraStartupMs = System.currentTimeMillis() - startRequestTimeMs
      startRequestTimeMs = 0
      logIfDebug()
    }
  }

  fun recordDecodeLatency(latencyMs: Long) {
    lastDecodeLatencyMs = latencyMs
  }

  fun incrementScanCount() {
    scanCount++
    logIfDebug()
  }

  fun toMap(): Map<String, Any> = mapOf(
    "cameraStartupMs" to cameraStartupMs,
    "lastDecodeLatencyMs" to lastDecodeLatencyMs,
    "scanCount" to scanCount,
  )

  private fun logIfDebug() {
    if (BuildConfig.DEBUG) {
      Log.d(
        TAG,
        "startup=${cameraStartupMs}ms decode=${lastDecodeLatencyMs}ms scans=$scanCount",
      )
    }
  }

  private const val TAG = "ScannerMetrics"
}
