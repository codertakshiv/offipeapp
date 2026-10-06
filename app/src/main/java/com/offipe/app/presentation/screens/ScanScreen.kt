package com.offipe.app.presentation.screens

import android.Manifest
import android.content.pm.PackageManager
import android.view.HapticFeedbackConstants
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.offipe.app.platform.QrScannerManager
import com.offipe.app.presentation.permissions.openAppDetailsSettings
import com.offipe.app.presentation.ui.components.CornerFrame
import com.offipe.app.presentation.ui.components.GhostActionBar
import com.offipe.app.presentation.ui.components.IconKey
import com.offipe.app.presentation.ui.components.PrimaryActionBar
import com.offipe.app.presentation.ui.theme.OffipeColors
import com.offipe.app.presentation.ui.theme.OffipeType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * SCAN — point at a UPI QR.
 *
 * Structure (per the reference):
 *  1. Top bar: close key left. Exactly ONE torch control lives in the
 *     bottom action row (labeled, with a live active state).
 *  2. A framed square viewfinder — dimmed surround, hairline frame and
 *     breathing corner brackets with corner illumination (no laser);
 *     tap anywhere on the preview to focus (real metering, not a
 *     decorative ring).
 *  3. Caption: POINT AT A UPI QR CODE / SUPPORTS UPI QR · GALLERY IMPORT.
 *  4. Bottom row: Torch / Gallery circle keys.
 *
 * The torch button drives the real flash through
 * [QrScannerManager.setTorch]; its active state mirrors
 * [QrScannerManager.onTorchStateChanged] (hardware truth), so the icon
 * only lights when the flash is actually on.
 */
@Composable
fun ScanScreen(
    qrManager: QrScannerManager,
    onResult: (String) -> Unit,
    onClose: () -> Unit,
    onOpenFaq: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var permissionDenied by remember { mutableStateOf(false) }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (!granted) permissionDenied = true
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) cameraLauncher.launch(Manifest.permission.CAMERA)
    }

    Box(
        modifier
            .fillMaxSize()
            .background(OffipeColors.Black)
    ) {
        when {
            hasCameraPermission -> CameraScannerContent(
                qrManager = qrManager,
                onResult = onResult,
                onClose = onClose
            )
            else -> PermissionDeniedContent(
                permanentlyDenied = permissionDenied,
                onGrant = { cameraLauncher.launch(Manifest.permission.CAMERA) },
                onOpenSettings = { openAppDetailsSettings(context) },
                onClose = onClose
            )
        }
    }
}

@Composable
private fun CameraScannerContent(
    qrManager: QrScannerManager,
    onResult: (String) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()

    var zoomRatio by remember { mutableFloatStateOf(1f) }
    var torchOn by remember { mutableStateOf(false) }
    var detected by remember { mutableStateOf(false) }
    var focusTap by remember { mutableStateOf<Offset?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                qrManager.decodeFromUri(context, uri)?.let { raw ->
                    view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                    detected = true
                    delay(180)
                    onResult(raw)
                }
            }
        }
    }

    val previewView = remember {
        PreviewView(context).apply {
            // COMPATIBLE => TextureView. The default SurfaceView punches a
            // separate layer through the window, which on several devices
            // suppresses the system's edge back gesture (swipe-back did
            // nothing while the in-app keys kept working). TextureView keeps
            // the preview inside the normal view hierarchy, so back works.
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }

    DisposableEffect(lifecycleOwner) {
        // The single torch control tracks the hardware torch state.
        qrManager.onTorchStateChanged = { on -> torchOn = on }
        qrManager.bindToLifecycle(lifecycleOwner, previewView) { raw ->
            if (!detected) {
                detected = true
                view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                // Brief 180ms hold so the lock flash registers, then hand
                // off to the QR result screen.
                scope.launch {
                    delay(180)
                    onResult(raw)
                }
            }
        }
        onDispose {
            qrManager.unbind()
            qrManager.onTorchStateChanged = null
        }
    }

    fun toggleTorch() {
        // State itself is set by onTorchStateChanged once the camera
        // confirms the flash actually flipped.
        if (qrManager.setTorch(!torchOn)) {
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        }
    }

    Box(Modifier.fillMaxSize()) {
        // Camera preview. Gesture handling (pinch-zoom / tap-to-focus) lives on
        // a sibling overlay instead of the AndroidView node, so the preview only
        // ever renders frames and never participates in pointer dispatch.
        androidx.compose.ui.viewinterop.AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )

        // Pinch-to-zoom and tap-to-focus, drawn over the preview but under the
        // torch / gallery controls so those stay clickable.
        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, _, zoomDelta, _ ->
                        // A straight pan reports zoomDelta == 1: skip it so a
                        // plain swipe (or the back gesture) doesn't queue a
                        // camera-control request on every move event.
                        if (zoomDelta != 1f) {
                            zoomRatio = (zoomRatio * zoomDelta).coerceIn(
                                QrScannerManager.MIN_ZOOM,
                                QrScannerManager.MAX_ZOOM
                            )
                            qrManager.setZoomRatio(zoomRatio)
                        }
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures { off ->
                        // Real focus + metering at the tapped point.
                        qrManager.focusAt(off.x, off.y)
                        focusTap = off
                    }
                }
        )

        // Framed viewfinder (static dim layer + animated corner layer)
        FrameViewfinder(detected = detected)

        // Fade-to-black hand-off on detection
        DetectionFadeOverlay(visible = detected)

        // Focus ring animation (visual feedback for the metering tap)
        focusTap?.let { tap ->
            FocusRing(at = tap, key = tap.toString(), onDone = { focusTap = null })
        }

        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // ── Top bar ──
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconKey(
                    icon = Icons.Default.Close,
                    contentDescription = "Close",
                    onClick = onClose,
                    size = 44.dp,
                    tint = OffipeColors.TextSecondary
                )
                Spacer(Modifier.weight(1f))
                if (!qrManager.flashAvailable) {
                    Text(
                        text = "No torch",
                        style = OffipeType.TerminalLabel,
                        color = OffipeColors.TextMuted
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            // ── Caption ──
            ScanCaption(detected = detected)

            Spacer(Modifier.height(26.dp))

            // ── Bottom action row ──
            Row(
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 40.dp, vertical = 22.dp),
                horizontalArrangement = Arrangement.spacedBy(
                    58.dp,
                    Alignment.CenterHorizontally
                ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircleAction(
                    icon = if (torchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                    label = if (torchOn) "Torch on" else "Torch",
                    active = torchOn,
                    onClick = ::toggleTorch
                )
                CircleAction(
                    icon = Icons.Default.PhotoLibrary,
                    label = "Gallery",
                    onClick = {
                        galleryLauncher.launch(
                            PickVisualMediaRequest(
                                ActivityResultContracts.PickVisualMedia.ImageOnly
                            )
                        )
                    }
                )
            }
        }
    }
}

// ─── Permission denied ────────────────────────────────────────────────────────

@Composable
private fun PermissionDeniedContent(
    permanentlyDenied: Boolean,
    onGrant: () -> Unit,
    onOpenSettings: () -> Unit,
    onClose: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CornerFrame(Modifier.fillMaxWidth()) {
            Column(
                Modifier.padding(horizontal = 22.dp, vertical = 26.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.PhotoLibrary,
                    contentDescription = null,
                    tint = OffipeColors.TextSecondary,
                    modifier = Modifier.size(34.dp)
                )
                Spacer(Modifier.height(18.dp))
                Text(
                    text = "Camera access required",
                    style = OffipeType.TitleLarge,
                    color = OffipeColors.TextPrimary
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Offipe scans UPI QR codes to autofill payment details. " +
                        "Grant camera access to continue.",
                    style = OffipeType.BodyMedium,
                    color = OffipeColors.TextSecondary,
                    textAlign = TextAlign.Center
                )
            }
        }
        Spacer(Modifier.height(20.dp))
        PrimaryActionBar(
            text = if (permanentlyDenied) "Open app settings" else "Grant camera",
            onClick = if (permanentlyDenied) onOpenSettings else onGrant,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(10.dp))
        GhostActionBar(
            text = "Cancel",
            onClick = onClose,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// ─── Viewfinder ───────────────────────────────────────────────────────────────

/**
 * Viewfinder feedback — deliberately built in two layers so the always-on
 * "searching" animation never repaints the full screen (the camera preview
 * must never stutter because of the overlay):
 *
 *  1. STATIC layer (full screen): dimmed surround, hairline frame and four
 *     small focus ticks at the edge midpoints. It only redraws when
 *     [detected] flips.
 *  2. ANIMATED layer (frame-sized): corner brackets that breathe, soft
 *     white corner illumination and a faint inner wash — the ONE continuous
 *     animation left in the scanner. It locks to the pastel-green state the
 *     moment a code is recognised.
 *
 * There is intentionally no sweeping laser line: searching is communicated
 * by the frame itself, in the app's black/white/pastel language.
 */
@Composable
private fun FrameViewfinder(detected: Boolean) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        // Same framing maths as before: 72% of width, capped at 42% of height.
        val side = minOf(maxWidth * 0.72f, maxHeight * 0.42f)

        // ── Static layer — dim, hairline frame, focus ticks ──
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val left = (w - side.toPx()) / 2f
            val top = (h - side.toPx()) / 2f
            val sidePx = side.toPx()

            val frame = Path().apply {
                addRect(Rect(left, top, left + sidePx, top + sidePx))
            }

            // Dim everything outside the frame
            val cutout = Path().apply {
                addRect(Rect(0f, 0f, w, h))
                addRect(Rect(left, top, left + sidePx, top + sidePx))
                fillType = PathFillType.EvenOdd
            }
            drawPath(cutout, color = Color(0x99000000))

            // Hairline frame — the quiet "target" of the scanner
            drawPath(
                path = frame,
                color = if (detected) OffipeColors.Mark else Color.White.copy(alpha = 0.14f),
                style = Stroke(width = 1f)
            )

            // Focus ticks at each edge midpoint (static recognition cue)
            val tick = 10.dp.toPx()
            val tickColor =
                if (detected) OffipeColors.Mark else Color.White.copy(alpha = 0.35f)
            val cx = left + sidePx / 2f
            val cy = top + sidePx / 2f
            drawLine(tickColor, Offset(cx - tick, top), Offset(cx + tick, top),
                strokeWidth = 1.5f)
            drawLine(tickColor, Offset(cx - tick, top + sidePx), Offset(cx + tick, top + sidePx),
                strokeWidth = 1.5f)
            drawLine(tickColor, Offset(left, cy - tick), Offset(left, cy + tick),
                strokeWidth = 1.5f)
            drawLine(tickColor, Offset(left + sidePx, cy - tick), Offset(left + sidePx, cy + tick),
                strokeWidth = 1.5f)

            // Lock tint
            if (detected) {
                drawPath(frame, color = OffipeColors.Mark.copy(alpha = 0.16f))
            }
        }

        // ── Animated layer — one shared pulse, confined to the frame ──
        val infinite = rememberInfiniteTransition(label = "scan")
        // Read inside the draw block below so the frame breathes without
        // recomposing the scanner on every animation frame.
        val searchState = infinite.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1700, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "search"
        )
        val lockState = animateFloatAsState(
            targetValue = if (detected) 1f else 0f,
            animationSpec = tween(durationMillis = 240),
            label = "lock"
        )

        Canvas(
            Modifier
                .align(Alignment.Center)
                .size(side)
        ) {
            val s = size.width
            val search = searchState.value.coerceIn(0f, 1f)
            val lock = lockState.value.coerceIn(0f, 1f)
            val brackets = OffipeColors.TextPrimary
            val active = OffipeColors.Mark

            // Faint inner wash while searching; cleared on lock
            if (lock < 1f) {
                drawRect(
                    color = Color.White.copy(alpha = 0.03f * search * (1f - lock)),
                    size = size
                )
            }

            // Soft corner illumination — two stacked discs, no neon
            if (lock < 1f) {
                val glowOuter = 18.dp.toPx()
                val glowInner = 9.dp.toPx()
                listOf(
                    Offset(0f, 0f), Offset(s, 0f),
                    Offset(0f, s), Offset(s, s)
                ).forEach { c ->
                    drawCircle(
                        color = Color.White.copy(alpha = 0.045f * search * (1f - lock)),
                        radius = glowOuter,
                        center = c
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = 0.075f * search * (1f - lock)),
                        radius = glowInner,
                        center = c
                    )
                }
            }

            // Corner brackets
            val bracket = s * 0.22f
            val wPx = 4.dp.toPx()
            val baseAlpha = 0.5f + 0.5f * search
            val lineColor = lerp(brackets, active, lock)
                .copy(alpha = (baseAlpha * (1f - lock) + lock).coerceIn(0f, 1f))
            listOf(
                Triple(Offset(0f, 0f), Offset(bracket, 0f), Offset(0f, bracket)),
                Triple(Offset(s, 0f), Offset(s - bracket, 0f), Offset(s, bracket)),
                Triple(Offset(0f, s), Offset(bracket, s), Offset(0f, s - bracket)),
                Triple(Offset(s, s), Offset(s - bracket, s), Offset(s, s - bracket))
            ).forEach { (corner, hEnd, vEnd) ->
                drawLine(lineColor, corner, hEnd, strokeWidth = wPx, cap = StrokeCap.Round)
                drawLine(lineColor, corner, vEnd, strokeWidth = wPx, cap = StrokeCap.Round)
            }

            // Corner LED tips — the recognition indicators
            val tip = 2.5.dp.toPx()
            listOf(Offset(0f, 0f), Offset(s, 0f), Offset(0f, s), Offset(s, s)).forEach { c ->
                drawCircle(
                    color = lerp(brackets, active, lock)
                        .copy(alpha = (0.45f * search + 0.55f * lock).coerceIn(0f, 1f)),
                    radius = tip,
                    center = c
                )
            }
        }
    }
}

/**
 * Brief fade-to-black overlay shown the moment a QR is detected. Smooths
 * the hand-off from the camera screen to the QR result screen.
 */
@Composable
private fun DetectionFadeOverlay(visible: Boolean) {
    val alpha by animateFloatAsState(
        targetValue = if (visible) 0.55f else 0f,
        animationSpec = tween(durationMillis = 220, easing = LinearEasing),
        label = "scan_fade"
    )
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = alpha))
    )
}

@Composable
private fun ScanCaption(detected: Boolean) {
    // Static copy — the breathing work lives in the frame layer, so the
    // caption no longer recomposes on every animation frame.
    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (detected) "LOCK" else "Point at a UPI QR Code",
            style = OffipeType.HeadlineLarge,
            color = OffipeColors.TextPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = if (detected) "Decoding payload…"
            else "Supports UPI QR · Gallery import",
            style = OffipeType.BodySmall,
            color = OffipeColors.TextSecondary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun FocusRing(at: Offset, key: String, onDone: () -> Unit) {
    val progress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 800),
        label = "focus_$key",
        finishedListener = { onDone() }
    )
    Canvas(modifier = Modifier.fillMaxSize()) {
        val radius = (32.dp.toPx()) * (1f - progress * 0.25f)
        drawCircle(
            color = OffipeColors.Mark.copy(alpha = 1f - progress),
            radius = radius,
            center = at,
            style = Stroke(width = 2.dp.toPx())
        )
    }
}

// ─── Bottom row key ───────────────────────────────────────────────────────────

@Composable
private fun CircleAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    active: Boolean = false
) {
    val view = LocalView.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val face = when {
        active -> OffipeColors.Mark
        isPressed -> OffipeColors.SurfaceHigher
        else -> OffipeColors.SurfaceHigh
    }
    val tint = when {
        active -> OffipeColors.Black
        else -> OffipeColors.TextPrimary
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .size(58.dp)
                .clip(CircleShape)
                .background(face)
                .clickable(interactionSource = interactionSource, indication = null) {
                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    onClick()
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(Modifier.height(7.dp))
        Text(
            text = label,
            style = OffipeType.BodySmall,
            color = OffipeColors.TextSecondary
        )
    }
}
