package com.offipe.app.presentation.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import com.offipe.app.domain.SessionState
import com.offipe.app.presentation.ui.theme.OffipeColors
import com.offipe.app.presentation.ui.theme.OffipeType
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlinx.coroutines.delay

/**
 * Session overlay — the USSD session taken over the whole viewport.
 *
 * Rendered once at the scaffold level while either ViewModel's session is
 * live, so every screen keeps its form underneath:
 *
 *  - Running: label, STEP x/y progress rail, ABORT pill.
 *  - Success: pastel-green textured beacon — pulsing rings, scaling disc,
 *    stroke-drawn check + sparkles — result copy, DONE pill.
 *  - Failed: pastel-red textured beacon — same rings/disc, slam-in cross,
 *    column jolt — message + raw result, WHY / RETRY.
 *
 * Result treatments are presentation only: they still render exactly the
 * `SessionState.Success` / `SessionState.Failed` data they are given, and
 * stick to the pastel-green / pastel-red accents (no new colours).
 */
@Composable
fun SessionOverlay(
    state: SessionState,
    onCancel: () -> Unit,
    onDone: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    successLabel: String = "PAYMENT AUTHORIZED",
    onWhyFailed: (() -> Unit)? = null
) {
    Box(
        modifier
            .fillMaxSize()
            .background(OffipeColors.Black)
    ) {
        when (state) {
            is SessionState.Idle -> Unit

            is SessionState.Running -> RunningSession(state, onCancel)

            is SessionState.Success -> ResultSession(
                state = state,
                successLabel = successLabel,
                onDone = onDone,
                modifier = modifier
            )

            is SessionState.Failed -> FailedSession(
                state = state,
                onRetry = onRetry,
                onWhyFailed = onWhyFailed,
                modifier = modifier
            )
        }
    }
}

// ─── Running ──────────────────────────────────────────────────────────────────

@Composable
private fun RunningSession(state: SessionState.Running, onCancel: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp)
    ) {
        Spacer(Modifier.height(20.dp))
        Tag(
            "Session live",
            led = OffipeColors.Mark,
            ledPulse = true,
            color = OffipeColors.TextSecondary
        )
        Spacer(Modifier.weight(1f))

        Text(
            text = state.label,
            style = OffipeType.DisplayMedium,
            color = OffipeColors.TextPrimary
        )
        Spacer(Modifier.height(18.dp))

        Tag("Step ${state.stepIndex} / ${state.total}")

        Spacer(Modifier.height(12.dp))

        // Segmented progress rail
        Row(
            Modifier
                .fillMaxWidth()
                .height(6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            repeat(state.total) { i ->
                val done = i < state.stepIndex
                val live = i == state.stepIndex
                Box(
                    Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                done -> OffipeColors.WaveSoft
                                live -> OffipeColors.Mark
                                else -> OffipeColors.Border
                            }
                        )
                )
            }
        }

        Spacer(Modifier.weight(1f))

        GhostActionBar(
            text = "Abort",
            onClick = onCancel,
            danger = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
    }
}


// ─── Shared result beacon ─────────────────────────────────────────────────────

/**
 * Animated, textured result marker used by both terminal states.
 *
 * @param accent pastel green for success, pastel red for failure
 */
@Composable
private fun ResultBeacon(accent: Color, content: @Composable () -> Unit) {
    val enter = remember { Animatable(0f) }
    val ripple by rememberInfiniteTransition(label = "beacon")
        .animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                tween(1800, easing = LinearEasing),
                RepeatMode.Restart
            ),
            label = "phase"
        )

    LaunchedEffect(Unit) {
        enter.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = 320f))
    }

    val scale = 0.55f + 0.45f * enter.value

    Box(Modifier.size(220.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(220.dp)) {
            val c = center
            val discR = 48.dp.toPx()

            // Soft halo behind the disc
            drawCircle(
                color = accent.copy(alpha = 0.10f + 0.07f * enter.value),
                radius = discR * 2.0f
            )

            // Two ripple rings breathing outward, offset by half a cycle
            listOf(ripple, (ripple + 0.5f) % 1f).forEach { t ->
                drawCircle(
                    color = accent.copy(alpha = (1f - t) * 0.32f * enter.value),
                    radius = discR + t * discR * 1.15f,
                    style = Stroke(width = 2.dp.toPx())
                )
            }

            // Textured disc: gradient wash + fine diagonal hatch
            val disc = Path().apply {
                addOval(
                    Rect(c.x - discR, c.y - discR, c.x + discR, c.y + discR)
                )
            }
            clipPath(disc) {
                drawRect(
                    Brush.verticalGradient(
                        listOf(accent, accent.copy(alpha = 0.84f))
                    )
                )
                var x = c.x - discR * 2f
                while (x < c.x + discR * 2f) {
                    drawLine(
                        color = Color.Black.copy(alpha = 0.10f),
                        start = Offset(x, c.y - discR * 1.5f),
                        end = Offset(x + discR * 3f, c.y + discR * 1.5f),
                        strokeWidth = 1.dp.toPx()
                    )
                    x += 5.dp.toPx()
                }
            }
            drawCircle(
                color = accent.copy(alpha = 0.95f),
                radius = discR,
                style = Stroke(width = 1.5.dp.toPx())
            )

            // Orbiting sparkle marks — diamonds and dashes
            val sparkles = listOf(
                Triple(38f, 1.70f, 0f),
                Triple(128f, 1.95f, 0.35f),
                Triple(205f, 1.75f, 0.7f),
                Triple(305f, 2.05f, 0.15f),
                Triple(72f, 2.35f, 0.55f),
                Triple(250f, 2.30f, 0.85f)
            )
            sparkles.forEach { (deg, radiusFactor, offset) ->
                val ang = Math.toRadians(deg.toDouble())
                val rad = discR * radiusFactor
                val px = c.x + (cos(ang) * rad).toFloat()
                val py = c.y + (sin(ang) * rad).toFloat()
                val shimmer = abs(sin((ripple + offset) * Math.PI * 2.0)).toFloat()
                val alpha = (0.20f + 0.55f * shimmer) * enter.value
                rotate(deg * 2f + ripple * 60f, Offset(px, py)) {
                    val d = 4.5.dp.toPx()
                    val diamond = Path().apply {
                        moveTo(px, py - d)
                        lineTo(px + d, py)
                        lineTo(px, py + d)
                        lineTo(px - d, py)
                        close()
                    }
                    drawPath(diamond, accent.copy(alpha = alpha))
                    drawLine(
                        color = accent.copy(alpha = alpha * 0.8f),
                        start = Offset(px + d * 2.4f, py - d * 1.6f),
                        end = Offset(px + d * 4.0f, py - d * 1.6f),
                        strokeWidth = 1.6.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }
        }

        Box(
            Modifier
                .size(96.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                },
            contentAlignment = Alignment.Center
        ) {
            content()
        }
    }
}

// ─── Success ──────────────────────────────────────────────────────────────────

@Composable
private fun ResultSession(
    state: SessionState.Success,
    successLabel: String,
    onDone: () -> Unit,
    modifier: Modifier
) {
    val view = LocalView.current
    val stroke = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        delay(150)
        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        stroke.animateTo(1f, tween(520, easing = LinearEasing))
    }

    Column(
        modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.weight(1f))

        // Green beacon: textured disc, rings and a stroke-drawn check
        ResultBeacon(accent = OffipeColors.Success) {
            Canvas(Modifier.size(96.dp)) {
                val w = size.width
                val h = size.height
                val a = Offset(w * 0.26f, h * 0.52f)
                val b = Offset(w * 0.43f, h * 0.70f)
                val c = Offset(w * 0.75f, h * 0.32f)
                val seg1 = hypot((b.x - a.x).toDouble(), (b.y - a.y).toDouble()).toFloat()
                val seg2 = hypot((c.x - b.x).toDouble(), (c.y - b.y).toDouble()).toFloat()
                val total = seg1 + seg2
                val drawn = total * stroke.value
                if (drawn > 0f) {
                    val first = drawn.coerceAtMost(seg1)
                    val firstT = if (seg1 > 0f) first / seg1 else 0f
                    drawLine(
                        color = OffipeColors.Black,
                        start = a,
                        end = Offset(
                            a.x + (b.x - a.x) * firstT,
                            a.y + (b.y - a.y) * firstT
                        ),
                        strokeWidth = 8f,
                        cap = StrokeCap.Round
                    )
                    if (drawn > seg1) {
                        val t = ((drawn - seg1) / seg2).coerceIn(0f, 1f)
                        drawLine(
                            color = OffipeColors.Black,
                            start = b,
                            end = Offset(
                                b.x + (c.x - b.x) * t,
                                b.y + (c.y - b.y) * t
                            ),
                            strokeWidth = 8f,
                            cap = StrokeCap.Round
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(26.dp))
        Text(
            text = successTitle(successLabel),
            style = OffipeType.DisplaySmall,
            color = OffipeColors.TextPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(14.dp))
        Text(
            text = state.resultText,
            style = OffipeType.BodyLarge,
            color = OffipeColors.TextSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.weight(1f))

        PrimaryActionBar(
            text = "Done",
            onClick = onDone,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
    }
}

private fun successTitle(label: String) = when {
    label.contains("BALANCE", ignoreCase = true) -> "Balance checked"
    else -> "Payment Successful"
}

// ─── Failed ───────────────────────────────────────────────────────────────────

@Composable
private fun FailedSession(
    state: SessionState.Failed,
    onRetry: () -> Unit,
    onWhyFailed: (() -> Unit)?,
    modifier: Modifier
) {
    val view = LocalView.current
    val shake = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        // Two decaying sideways jolts, then settle.
        shake.animateTo(1f, tween(340, easing = FastOutSlowInEasing))
        shake.animateTo(0f, tween(160))
        shake.animateTo(0.6f, tween(260, easing = FastOutSlowInEasing))
        shake.animateTo(0f, tween(220))
    }

    Column(
        modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp)
            .graphicsLayer {
                translationX = shake.value * 14f
                rotationZ = shake.value * -1.2f
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.weight(1f))

        // Red beacon: textured disc, rings and a slam-in cross
        ResultBeacon(accent = OffipeColors.Danger) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = null,
                tint = OffipeColors.Black,
                modifier = Modifier.size(46.dp)
            )
        }

        Spacer(Modifier.height(26.dp))
        Text(
            text = "Payment Failed",
            style = OffipeType.DisplaySmall,
            color = OffipeColors.TextPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = state.message,
            style = OffipeType.BodyLarge,
            color = OffipeColors.TextSecondary,
            textAlign = TextAlign.Center
        )
        if (state.resultText.isNotBlank() && state.resultText != state.message) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = state.resultText,
                style = OffipeType.BodyMedium,
                color = OffipeColors.TextMuted,
                textAlign = TextAlign.Center
            )
        }

        Spacer(Modifier.weight(1f))

        if (onWhyFailed != null) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                GhostActionBar(
                    text = "Why?",
                    onClick = onWhyFailed,
                    modifier = Modifier.weight(1f)
                )
                PrimaryActionBar(
                    text = "Retry",
                    onClick = onRetry,
                    modifier = Modifier.weight(1f)
                )
            }
        } else {
            PrimaryActionBar(
                text = "Retry",
                onClick = onRetry,
                modifier = Modifier.fillMaxWidth()
            )
        }
        Spacer(Modifier.height(12.dp))
    }
}
