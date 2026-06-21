package expo.modules.logisticsscanner.scanner

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.SurfaceTexture
import android.util.Log
import android.view.Surface
import androidx.camera.core.Camera
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraState
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.UseCaseGroup
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

private const val TAG = "CameraScannerProvider"

class CameraScannerProvider(
  private val context: Context,
  private val lifecycleOwner: LifecycleOwner,
  private val previewView: PreviewView,
) : ScannerProvider {
  private var camera: Camera? = null
  private var cameraProvider: ProcessCameraProvider? = null
  private var analyzer: BarcodeAnalyzer? = null
  private var analysisExecutor: ExecutorService? = null
  private var previewSurfaceTexture: SurfaceTexture? = null
  private var torchEnabled = false
  private var onScanCallback: ((ScanResult) -> Unit)? = null
  private var onStartedCallback: (() -> Unit)? = null
  private var onFailedCallback: (() -> Unit)? = null

  override fun start(
    onScan: (ScanResult) -> Unit,
    onStarted: () -> Unit,
    onFailed: () -> Unit,
  ) {
    if (!hasCameraPermission()) {
      Log.w(TAG, "Camera permission not granted")
      onFailed()
      return
    }

    onScanCallback = onScan
    onStartedCallback = onStarted
    onFailedCallback = onFailed

    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
    cameraProviderFuture.addListener(
      {
        try {
          val provider = cameraProviderFuture.get()
          cameraProvider = provider
          bindUseCases(provider)
        } catch (error: Exception) {
          Log.e(TAG, "Failed to start camera", error)
          onFailedCallback?.invoke()
        }
      },
      ContextCompat.getMainExecutor(context),
    )
  }

  override fun stop() {
    try {
      cameraProvider?.unbindAll()
    } catch (error: Exception) {
      Log.w(TAG, "Failed to unbind camera", error)
    }

    analyzer?.close()
    analyzer = null

    analysisExecutor?.shutdownNow()
    analysisExecutor = null

    camera = null
    cameraProvider = null
    onScanCallback = null
    onStartedCallback = null
    onFailedCallback = null
  }

  override fun setTorch(enabled: Boolean) {
    torchEnabled = enabled
    applyTorch()
  }

  override fun setPreviewSurfaceTexture(texture: SurfaceTexture?) {
    previewSurfaceTexture = texture
    val provider = cameraProvider
    if (provider != null && onScanCallback != null) {
      bindUseCases(provider)
    }
  }

  private fun bindUseCases(provider: ProcessCameraProvider) {
    if (!hasCameraPermission()) {
      Log.w(TAG, "Camera permission not granted")
      onFailedCallback?.invoke()
      return
    }

    provider.unbindAll()

    val preview = Preview.Builder().build()
    configurePreviewSurface(preview)

    val imageAnalysis = ImageAnalysis.Builder()
      .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
      .build()

    val executor = Executors.newSingleThreadExecutor()
    analysisExecutor?.shutdownNow()
    analysisExecutor = executor

    val callback = onScanCallback ?: return
    val barcodeAnalyzer = BarcodeAnalyzer(callback)
    analyzer?.close()
    analyzer = barcodeAnalyzer
    imageAnalysis.setAnalyzer(executor, barcodeAnalyzer)

    try {
      val boundCamera = if (previewView.width > 0 && previewView.height > 0) {
        val viewPort = previewView.viewPort
        if (viewPort != null) {
          val useCaseGroup = UseCaseGroup.Builder()
            .setViewPort(viewPort)
            .addUseCase(preview)
            .addUseCase(imageAnalysis)
            .build()
          provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, useCaseGroup)
        } else {
          provider.bindToLifecycle(
            lifecycleOwner,
            CameraSelector.DEFAULT_BACK_CAMERA,
            preview,
            imageAnalysis,
          )
        }
      } else {
        provider.bindToLifecycle(
          lifecycleOwner,
          CameraSelector.DEFAULT_BACK_CAMERA,
          preview,
          imageAnalysis,
        )
      }

      camera = boundCamera
      observeCameraState(boundCamera.cameraInfo)
      onStartedCallback?.invoke()
    } catch (error: Exception) {
      Log.e(TAG, "Failed to bind camera use cases", error)
      onFailedCallback?.invoke()
    }
  }

  private fun configurePreviewSurface(preview: Preview) {
    val texture = previewSurfaceTexture
    if (texture != null) {
      val previewWidth = previewView.width.coerceAtLeast(1)
      val previewHeight = previewView.height.coerceAtLeast(1)
      texture.setDefaultBufferSize(previewWidth, previewHeight)
      preview.setSurfaceProvider { request ->
        val surface = Surface(texture)
        request.provideSurface(surface, ContextCompat.getMainExecutor(context)) {
          surface.release()
        }
      }
      return
    }

    preview.surfaceProvider = previewView.surfaceProvider
  }

  private fun observeCameraState(cameraInfo: CameraInfo) {
    cameraInfo.cameraState.observe(lifecycleOwner) { state ->
      if (state.type == CameraState.Type.OPEN) {
        applyTorch()
      }
    }
  }

  private fun applyTorch() {
    val activeCamera = camera ?: return
    if (!activeCamera.cameraInfo.hasFlashUnit()) {
      Log.w(TAG, "Torch requested but device has no flash unit")
      return
    }

    try {
      activeCamera.cameraControl.enableTorch(torchEnabled)
    } catch (error: Exception) {
      Log.w(TAG, "Failed to set torch state", error)
    }
  }

  private fun hasCameraPermission(): Boolean {
    return ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
      PackageManager.PERMISSION_GRANTED
  }
}
