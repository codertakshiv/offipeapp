package com.offipe.app.platform

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.camera.core.Camera
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.MeteringPointFactory
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors
import kotlin.coroutines.resume

/**
 * Manages CameraX camera preview and ML Kit barcode scanning for QR codes.
 *
 * Supports:
 * - Real-time camera scanning with rear-facing camera
 * - Zoom ratio control (1.0x to 3.0x)
 * - Gallery image decoding (JPEG, PNG, GIF, WebP)
 */
class QrScannerManager {

    private var cameraProvider: ProcessCameraProvider? = null
    private var cameraControl: CameraControl? = null
    private var camera: Camera? = null
    private var previewViewRef: PreviewView? = null
    private var torchObserver: Observer<Int>? = null

    /**
     * Bumped by [unbind]; a camera-provider callback captured before that is
     * stale and must not bind. Without it a slow listener could attach the
     * camera to an already-disposed owner, leaving the next scan black.
     */
    private var bindGeneration = 0
    private val analysisExecutor = Executors.newSingleThreadExecutor()

    /**
     * Mirror of the hardware torch state (from `CameraInfo.torchState`),
     * so the UI's single flash control always reflects reality rather
     * than an optimistic local flag.
     */
    var isTorchOn: Boolean = false
        private set

    /**
     * Invoked on the main thread whenever the hardware torch state
     * changes (system toggles included).
     */
    var onTorchStateChanged: ((Boolean) -> Unit)? = null

    /**
     * Whether the bound camera reports a flash unit at all.
     *
     * Held as Compose state rather than a plain query: binding completes after
     * the first composition, so a plain `hasFlash()` call would keep answering
     * "no camera yet" until some unrelated state change forced a recompose.
     */
    var flashAvailable: Boolean by mutableStateOf(false)
        private set

    private val scannerOptions = BarcodeScannerOptions.Builder()
        .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
        .build()

    private val barcodeScanner = BarcodeScanning.getClient(scannerOptions)

    /**
     * Binds camera preview and barcode analysis to the given lifecycle.
     * Uses the rear-facing camera.
     *
     * @param lifecycleOwner Lifecycle to bind the camera to
     * @param previewView The PreviewView to display the camera feed
     * @param onResult Callback invoked with the decoded QR string on detection
     */
    @OptIn(markerClass = [ExperimentalGetImage::class])
    fun bindToLifecycle(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        onResult: (String) -> Unit
    ) {
        val context = previewView.context
        val generation = ++bindGeneration
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            if (generation != bindGeneration) {
                // The screen went away while the provider was resolving.
                runCatching { cameraProviderFuture.get().unbindAll() }
                return@addListener
            }
            val provider = cameraProviderFuture.get()
            cameraProvider = provider

            val preview = Preview.Builder()
                .build()
                .also { it.surfaceProvider = previewView.surfaceProvider }

            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            var resultDelivered = false

            imageAnalysis.setAnalyzer(analysisExecutor) { imageProxy ->
                val mediaImage = imageProxy.image
                if (mediaImage != null && !resultDelivered) {
                    val inputImage = InputImage.fromMediaImage(
                        mediaImage,
                        imageProxy.imageInfo.rotationDegrees
                    )
                    barcodeScanner.process(inputImage)
                        .addOnSuccessListener { barcodes ->
                            val qrValue = barcodes.firstOrNull()?.rawValue
                            if (qrValue != null && !resultDelivered) {
                                resultDelivered = true
                                onResult(qrValue)
                            }
                        }
                        .addOnCompleteListener {
                            imageProxy.close()
                        }
                } else {
                    imageProxy.close()
                }
            }

            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build()

            provider.unbindAll()
            val camera = provider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageAnalysis
            )
            this.camera = camera
            this.cameraControl = camera.cameraControl
            this.previewViewRef = previewView
            // Publish flash capability once the camera is actually bound so the
            // scanner's torch affordance is never a frame behind reality.
            flashAvailable = camera.cameraInfo.hasFlashUnit()

            // Mirror the real torch state so the UI control stays in sync.
            torchObserver?.let { camera.cameraInfo.torchState.removeObserver(it) }
            val observer = Observer<Int> { state ->
                // CameraInfo#getTorchState: 0 = off, 1 = on.
                val on = state == TORCH_STATE_ON
                isTorchOn = on
                onTorchStateChanged?.invoke(on)
            }
            torchObserver = observer
            camera.cameraInfo.torchState.observe(lifecycleOwner, observer)
            onTorchStateChanged?.invoke(isTorchOn)
        }, ContextCompat.getMainExecutor(context))
    }

    /**
     * Sets the camera zoom ratio.
     *
     * @param ratio Zoom level clamped between 1.0x and 3.0x
     */
    fun setZoomRatio(ratio: Float) {
        val clamped = ratio.coerceIn(MIN_ZOOM, MAX_ZOOM)
        cameraControl?.setZoomRatio(clamped)
    }

    /**
     * Requests the torch (flashlight) state. No-op on devices without a
     * flash. Returns true when the request was submitted — the confirmed
     * state arrives through [onTorchStateChanged].
     */
    fun setTorch(enabled: Boolean): Boolean {
        val cam = camera ?: return false
        if (!cam.cameraInfo.hasFlashUnit()) return false
        return try {
            cam.cameraControl.enableTorch(enabled)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Starts focus + metering at a point given in the preview view's
     * coordinate space (e.g. the offset of a tap gesture).
     *
     * @return true if the focus action was submitted
     */
    fun focusAt(x: Float, y: Float): Boolean {
        val view = previewViewRef ?: return false
        val control = cameraControl ?: return false
        return try {
            val factory: MeteringPointFactory = view.meteringPointFactory
            val point = factory.createPoint(x, y)
            control.startFocusAndMetering(FocusMeteringAction.Builder(point).build())
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Unbinds all camera use cases and releases the camera.
     */
    fun unbind() {
        bindGeneration++
        val cam = camera
        torchObserver?.let { observer ->
            cam?.cameraInfo?.torchState?.removeObserver(observer)
        }
        torchObserver = null
        cameraProvider?.unbindAll()
        cameraProvider = null
        cameraControl = null
        camera = null
        previewViewRef = null
        flashAvailable = false
        isTorchOn = false
        onTorchStateChanged?.invoke(false)
    }

    /**
     * Decodes a QR code from a gallery image URI.
     * Supports JPEG, PNG, GIF, and WebP formats.
     *
     * @param context Android context for content resolver access
     * @param uri URI of the image to decode
     * @return The decoded QR string, or null if no QR code was found
     */
    suspend fun decodeFromUri(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
        // Decoding + ML Kit prep happen off the main thread so a big gallery
        // image never janks the scanner UI.
        suspendCancellableCoroutine { continuation ->
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream == null) {
                    continuation.resume(null)
                    return@suspendCancellableCoroutine
                }

                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream.close()

                if (bitmap == null) {
                    continuation.resume(null)
                    return@suspendCancellableCoroutine
                }

                val inputImage = InputImage.fromBitmap(bitmap, 0)
                barcodeScanner.process(inputImage)
                    .addOnSuccessListener { barcodes ->
                        val qrValue = barcodes.firstOrNull()?.rawValue
                        if (continuation.isActive) {
                            continuation.resume(qrValue)
                        }
                    }
                    .addOnFailureListener {
                        if (continuation.isActive) {
                            continuation.resume(null)
                        }
                    }
            } catch (e: Exception) {
                if (continuation.isActive) {
                    continuation.resume(null)
                }
            }
        }
    }

    companion object {
        const val MIN_ZOOM = 1.0f
        const val MAX_ZOOM = 3.0f

        /** `CameraInfo.getTorchState()` value when the flash is lit. */
        private const val TORCH_STATE_ON = 1
    }
}