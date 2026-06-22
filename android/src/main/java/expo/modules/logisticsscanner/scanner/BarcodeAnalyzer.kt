package expo.modules.logisticsscanner.scanner

import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.atomic.AtomicBoolean

private const val TAG = "BarcodeAnalyzer"

internal object ScanFormats {
  const val CODE_128 = Barcode.FORMAT_CODE_128
}

class BarcodeAnalyzer(
  private val onScan: (ScanResult) -> Unit,
  private val onFailure: (ScanError) -> Unit = {},
) : ImageAnalysis.Analyzer {
  private val isProcessing = AtomicBoolean(false)
  private val mainHandler = Handler(Looper.getMainLooper())

  private val scanner = BarcodeScanning.getClient(
    BarcodeScannerOptions.Builder()
      .setBarcodeFormats(ScanFormats.CODE_128)
      .build(),
  )

  override fun analyze(imageProxy: ImageProxy) {
    if (!isProcessing.compareAndSet(false, true)) {
      imageProxy.close()
      return
    }

    val decodeStartedAt = System.currentTimeMillis()
    val mediaImage = imageProxy.image
    if (mediaImage == null) {
      isProcessing.set(false)
      imageProxy.close()
      return
    }

    val inputImage = InputImage.fromMediaImage(
      mediaImage,
      imageProxy.imageInfo.rotationDegrees,
    )

    scanner.process(inputImage)
      .addOnSuccessListener { barcodes ->
        val barcode = barcodes.firstOrNull()
        if (barcode != null) {
          val value = barcode.rawValue
          if (value != null) {
            val decodeLatencyMs = System.currentTimeMillis() - decodeStartedAt
            ScannerMetrics.recordDecodeLatency(decodeLatencyMs)

            val result = ScanResult(
              value = value,
              format = "CODE_128",
              timestamp = System.currentTimeMillis(),
            )
            mainHandler.post { onScan(result) }
          }
        }
      }
      .addOnFailureListener { error ->
        Log.w(TAG, "Barcode analysis failed", error)
        mainHandler.post {
          onFailure(
            ScanError(
              code = ScanError.DECODER_FAILURE,
              message = error.message ?: "Barcode analysis failed",
            ),
          )
        }
      }
      .addOnCompleteListener {
        isProcessing.set(false)
        imageProxy.close()
      }
  }

  fun close() {
    scanner.close()
  }
}
