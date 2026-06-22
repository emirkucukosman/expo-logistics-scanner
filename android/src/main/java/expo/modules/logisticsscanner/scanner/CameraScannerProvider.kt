package expo.modules.logisticsscanner.scanner

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.SurfaceTexture
import android.os.Handler
import android.os.Looper
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

private const val TAG = "CameraScannerProvider"
private const val REBIND_DELAY_MS = 500L

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
  private var wantsRunning = false
  private var isPausedByBackground = false
  private var lifecycleObserverRegistered = false
  private val mainHandler = Handler(Looper.getMainLooper())

  private var onScanCallback: ((ScanResult) -> Unit)? = null
  private var onStartedCallback: (() -> Unit)? = null
  private var onFailedCallback: ((ScanError) -> Unit)? = null
  private var onErrorCallback: ((ScanError) -> Unit)? = null

  private val appLifecycleObserver = LifecycleEventObserver { _, event ->
    when (event) {
      Lifecycle.Event.ON_STOP -> pauseForBackground()
      Lifecycle.Event.ON_START -> resumeFromBackground()
      else -> {}
    }
  }

  override fun start(
    onScan: (ScanResult) -> Unit,
    onStarted: () -> Unit,
    onFailed: (ScanError) -> Unit,
    onError: (ScanError) -> Unit,
  ) {
    if (!hasCameraPermission()) {
      Log.w(TAG, "Camera permission not granted")
      onFailed(
        ScanError(
          code = ScanError.PERMISSION_DENIED,
          message = "Camera permission not granted",
        ),
      )
      return
    }

    wantsRunning = true
    isPausedByBackground = false
    onScanCallback = onScan
    onStartedCallback = onStarted
    onFailedCallback = onFailed
    onErrorCallback = onError
    registerAppLifecycleObserver()
    ScannerMetrics.markStartRequested()

    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
    cameraProviderFuture.addListener(
      {
        try {
          val provider = cameraProviderFuture.get()
          cameraProvider = provider
          bindUseCases(provider)
        } catch (error: Exception) {
          Log.e(TAG, "Failed to start camera", error)
          onFailedCallback?.invoke(
            ScanError(
              code = ScanError.CAMERA_UNAVAILABLE,
              message = error.message ?: "Failed to start camera",
            ),
          )
        }
      },
      ContextCompat.getMainExecutor(context),
    )
  }

  override fun stop() {
    wantsRunning = false
    isPausedByBackground = false
    unregisterAppLifecycleObserver()
    mainHandler.removeCallbacksAndMessages(null)

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
    onErrorCallback = null
  }

  override fun setTorch(enabled: Boolean) {
    torchEnabled = enabled
    applyTorch()
  }

  override fun setPreviewSurfaceTexture(texture: SurfaceTexture?) {
    previewSurfaceTexture = texture
    val provider = cameraProvider
    if (provider != null && onScanCallback != null && wantsRunning && !isPausedByBackground) {
      bindUseCases(provider)
    }
  }

  private fun pauseForBackground() {
    if (!wantsRunning || isPausedByBackground) {
      return
    }

    isPausedByBackground = true
    try {
      cameraProvider?.unbindAll()
    } catch (error: Exception) {
      Log.w(TAG, "Failed to pause camera for background", error)
    }
    camera = null
  }

  private fun resumeFromBackground() {
    if (!wantsRunning || !isPausedByBackground) {
      return
    }

    isPausedByBackground = false
    val provider = cameraProvider
    if (provider != null) {
      bindUseCases(provider)
    }
  }

  private fun bindUseCases(provider: ProcessCameraProvider) {
    if (!wantsRunning || isPausedByBackground) {
      return
    }

    if (!hasCameraPermission()) {
      Log.w(TAG, "Camera permission not granted")
      onFailedCallback?.invoke(
        ScanError(
          code = ScanError.PERMISSION_DENIED,
          message = "Camera permission not granted",
        ),
      )
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

    val scanCallback = onScanCallback ?: return
    val errorCallback = onErrorCallback ?: {}
    val barcodeAnalyzer = BarcodeAnalyzer(
      onScan = scanCallback,
      onFailure = errorCallback,
    )
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
      ScannerMetrics.markCameraStarted()
      onStartedCallback?.invoke()
    } catch (error: Exception) {
      Log.e(TAG, "Failed to bind camera use cases", error)
      onFailedCallback?.invoke(
        ScanError(
          code = ScanError.CAMERA_UNAVAILABLE,
          message = error.message ?: "Failed to bind camera",
        ),
      )
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
    cameraInfo.cameraState.removeObservers(lifecycleOwner)
    cameraInfo.cameraState.observe(lifecycleOwner) { state ->
      when (state.type) {
        CameraState.Type.OPEN -> applyTorch()
        CameraState.Type.CLOSED -> {
          if (wantsRunning && !isPausedByBackground && state.error != null) {
            handleCameraInterruption(state.error)
          }
        }
        else -> {}
      }
    }
  }

  private fun handleCameraInterruption(error: CameraState.StateError?) {
    Log.w(TAG, "Camera interrupted", error?.cause)
    onErrorCallback?.invoke(
      ScanError(
        code = ScanError.INTERRUPTED,
        message = error?.cause?.message ?: "Camera interrupted",
      ),
    )

    if (!wantsRunning || isPausedByBackground) {
      return
    }

    mainHandler.postDelayed({
      val provider = cameraProvider
      if (wantsRunning && !isPausedByBackground && provider != null) {
        bindUseCases(provider)
      }
    }, REBIND_DELAY_MS)
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

  private fun registerAppLifecycleObserver() {
    if (!lifecycleObserverRegistered) {
      ProcessLifecycleOwner.get().lifecycle.addObserver(appLifecycleObserver)
      lifecycleObserverRegistered = true
    }
  }

  private fun unregisterAppLifecycleObserver() {
    if (lifecycleObserverRegistered) {
      ProcessLifecycleOwner.get().lifecycle.removeObserver(appLifecycleObserver)
      lifecycleObserverRegistered = false
    }
  }

  private fun hasCameraPermission(): Boolean {
    return ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
      PackageManager.PERMISSION_GRANTED
  }
}
