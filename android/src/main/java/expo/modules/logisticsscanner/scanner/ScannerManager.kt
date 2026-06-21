package expo.modules.logisticsscanner.scanner

import android.content.Context
import android.graphics.SurfaceTexture
import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner

class ScannerManager(
  private val context: Context,
  private val lifecycleOwner: LifecycleOwner,
  private val previewView: PreviewView,
) {
  private val provider: CameraScannerProvider = CameraScannerProvider(
    context,
    lifecycleOwner,
    previewView,
  )

  fun start(onScan: (ScanResult) -> Unit) {
    start(onScan, onStarted = {}, onFailed = {})
  }

  fun start(
    onScan: (ScanResult) -> Unit,
    onStarted: () -> Unit,
    onFailed: () -> Unit,
  ) {
    provider.start(onScan, onStarted, onFailed)
  }

  fun stop() {
    provider.stop()
  }

  fun setTorch(enabled: Boolean) {
    provider.setTorch(enabled)
  }

  fun setPreviewSurfaceTexture(texture: SurfaceTexture?) {
    provider.setPreviewSurfaceTexture(texture)
  }
}
