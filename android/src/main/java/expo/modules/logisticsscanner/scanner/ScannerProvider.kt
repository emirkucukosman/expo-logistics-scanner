package expo.modules.logisticsscanner.scanner

import android.graphics.SurfaceTexture

interface ScannerProvider {
  fun start(
    onScan: (ScanResult) -> Unit,
    onStarted: () -> Unit = {},
    onFailed: () -> Unit = {},
  )
  fun stop()
  fun setTorch(enabled: Boolean)
  fun setPreviewSurfaceTexture(texture: SurfaceTexture?)
}
