package expo.modules.logisticsscanner.scanner

import android.content.Context
import android.graphics.SurfaceTexture
import android.util.Log
import android.view.View
import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.findViewTreeLifecycleOwner
import expo.modules.interfaces.camera.CameraViewInterface
import expo.modules.kotlin.AppContext
import expo.modules.kotlin.viewevent.EventDispatcher
import expo.modules.kotlin.views.ExpoView

private const val TAG = "ScannerView"

class ScannerView(context: Context, appContext: AppContext) :
  ExpoView(context, appContext),
  CameraViewInterface {
  private val onScan by EventDispatcher()
  private val onError by EventDispatcher()
  private val previewView = PreviewView(context).also {
    it.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
    it.scaleType = PreviewView.ScaleType.FILL_CENTER
    it.implementationMode = PreviewView.ImplementationMode.COMPATIBLE
    addView(it)
  }

  override val shouldUseAndroidLayout: Boolean = true

  private var scannerManager: ScannerManager? = null
  private var isScanning = false
  private var isStarting = false
  private var torchEnabled = false
  private var duplicateTimeoutMs = 0L
  private var lastScanValue: String? = null
  private var lastScanEmitTimeMs = 0L
  private var previewSurfaceTexture: SurfaceTexture? = null

  override fun setPreviewTexture(surfaceTexture: SurfaceTexture?) {
    previewSurfaceTexture = surfaceTexture
    scannerManager?.setPreviewSurfaceTexture(surfaceTexture)

    if (surfaceTexture != null) {
      post {
        stopScanning()
        tryStartScanning()
      }
    }
  }

  override fun getPreviewSizeAsArray(): IntArray {
    val previewWidth = if (width > 0) width else previewView.width
    val previewHeight = if (height > 0) height else previewView.height
    return intArrayOf(previewWidth, previewHeight)
  }

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    previewView.measure(widthMeasureSpec, heightMeasureSpec)
    setMeasuredDimension(
      android.view.ViewGroup.resolveSize(previewView.measuredWidth, widthMeasureSpec),
      android.view.ViewGroup.resolveSize(previewView.measuredHeight, heightMeasureSpec),
    )
  }

  override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
    super.onLayout(changed, left, top, right, bottom)
    val layoutWidth = right - left
    val layoutHeight = bottom - top
    previewView.layout(0, 0, layoutWidth, layoutHeight)

    if (layoutWidth > 0 && layoutHeight > 0) {
      post { tryStartScanning() }
    }
  }

  override fun onAttachedToWindow() {
    super.onAttachedToWindow()
    post { tryStartScanning() }
  }

  override fun onDetachedFromWindow() {
    stopScanning()
    super.onDetachedFromWindow()
  }

  fun startScanning() {
    post { tryStartScanning() }
  }

  fun setDuplicateTimeout(timeoutMs: Int) {
    duplicateTimeoutMs = timeoutMs.toLong().coerceAtLeast(0)
  }

  private fun tryStartScanning() {
    if (isScanning || isStarting || !isAttachedToWindow || width == 0 || height == 0) {
      return
    }

    val lifecycleOwner = resolveLifecycleOwner()
    if (lifecycleOwner == null) {
      Log.w(TAG, "Cannot start scanning: no lifecycle owner available")
      return
    }

    val manager = scannerManager ?: ScannerManager(
      appContext.currentActivity ?: previewView.context,
      lifecycleOwner,
      previewView,
    ).also {
      scannerManager = it
    }

    manager.setPreviewSurfaceTexture(previewSurfaceTexture)
    isStarting = true

    manager.start(
      onScan = { result -> handleScan(result) },
      onStarted = {
        isStarting = false
        isScanning = true
        manager.setTorch(torchEnabled)
      },
      onFailed = { error -> dispatchFatalError(error) },
      onError = { error -> dispatchRuntimeError(error) },
    )
  }

  private fun handleScan(result: ScanResult) {
    val now = System.currentTimeMillis()
    if (
      duplicateTimeoutMs > 0 &&
      result.value == lastScanValue &&
      now - lastScanEmitTimeMs < duplicateTimeoutMs
    ) {
      return
    }

    lastScanValue = result.value
    lastScanEmitTimeMs = now
    ScannerMetrics.incrementScanCount()

    onScan(
      mapOf(
        "value" to result.value,
        "format" to result.format,
        "timestamp" to result.timestamp,
      ),
    )
  }

  private fun dispatchFatalError(error: ScanError) {
    isStarting = false
    isScanning = false
    onError(error.toMap())
  }

  private fun dispatchRuntimeError(error: ScanError) {
    onError(error.toMap())
  }

  fun stopScanning() {
    if (!isScanning && !isStarting) {
      return
    }

    scannerManager?.stop()
    isScanning = false
    isStarting = false
  }

  fun setTorchEnabled(enabled: Boolean) {
    torchEnabled = enabled
    scannerManager?.setTorch(enabled)
  }

  private fun resolveLifecycleOwner(): LifecycleOwner? {
    (appContext.currentActivity as? LifecycleOwner)?.let { return it }
    findViewTreeLifecycleOwner()?.let { return it }

    var parentView: View? = parent as? View
    while (parentView != null) {
      val owner = parentView.findViewTreeLifecycleOwner()
      if (owner != null) {
        return owner
      }
      parentView = parentView.parent as? View
    }

    return null
  }
}
