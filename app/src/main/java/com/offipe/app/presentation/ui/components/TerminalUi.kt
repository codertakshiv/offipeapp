package com.offipe.app.presentation.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.offipe.app.presentation.ui.theme.OffipeColors
import com.offipe.app.presentation.ui.theme.OffipeType
import kotlinx.coroutines.delay

// ─── Structure ────────────────────────────────────────────────────────────────

/** A 1dp rule — the quiet separator of the whole system. */
@Composable
fun Hairline(
    modifier: Modifier = Modifier,
    color: Color = OffipeColors.Border
) {
    Box(
        modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(color)
    )
}

/** Pulsing status dot. Live states breathe; static states sit solid. */
@Composable
fun LedDot(
    size: Dp = 6.dp,
    color: Color = OffipeColors.TextSecondary,
    pulse: Boolean = false,
    modifier: Modifier = Modifier
) {
    // The animated value is read inside the graphicsLayer block so a
    // pulsing LED invalidates only its own layer — it never recomposes the
    // composable (which previously happened on every frame, for every
    // pulsing dot on screen).
    val alphaState: State<Float> = if (pulse) {
        rememberInfiniteTransition(label = "led").animateFloat(
            initialValue = 0.35f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(700),
                repeatMode = RepeatMode.Reverse
            ),
            label = "led_alpha"
        )
    } else {
        remember { mutableStateOf(1f) }
    }
    Box(
        modifier
            .size(size)
            .graphicsLayer { alpha = alphaState.value }
            .clip(CircleShape)
            .background(color)
    )
}

/** Small uppercase label — field labels, section overlines, tags. */
@Composable
fun Tag(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = OffipeColors.TextMuted,
    led: Color? = null,
    ledPulse: Boolean = false
) {
    Row(
        modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        if (led != null) {
            LedDot(size = 5.dp, color = led, pulse = ledPulse)
            Spacer(Modifier.width(7.dp))
        }
        Text(
            text = text.uppercase(),
            style = OffipeType.TerminalLabel,
            color = color
        )
    }
}

/** Outline chip — compact status readouts and inline secondary keys. */
@Composable
fun Chip(
    text: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    color: Color = OffipeColors.TextSecondary,
    fillColor: Color = Color.Transparent
) {
    val view = LocalView.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val clickable = onClick != null
    Box(
        modifier
            .clip(RoundedCornerShape(50))
            .background(if (isPressed && clickable) color.copy(alpha = 0.12f) else fillColor)
            .border(
                width = 1.dp,
                color = color.copy(alpha = if (isPressed) 0.9f else 0.5f),
                shape = RoundedCornerShape(50)
            )
            .then(
                if (clickable) {
                    Modifier
                        .defaultMinSize(minHeight = 44.dp, minWidth = 44.dp)
                        .clickable(interactionSource = interactionSource, indication = null) {
                            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                            onClick?.invoke()
                        }
                } else Modifier
            )
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = OffipeType.LabelSmall,
            color = color
        )
    }
}

// ─── Panels ───────────────────────────────────────────────────────────────────

/**
 * Rounded surface card — the primary container of the design language:
 * near-black face, thin border, 16dp radius.
 */
@Composable
fun PanelCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    borderColor: Color = OffipeColors.Border,
    fillColor: Color = OffipeColors.Surface,
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    Box(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (isPressed && onClick != null) fillColor.copy(alpha = 1f) else fillColor)
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .then(
                if (onClick != null) {
                    Modifier.clickable(interactionSource = interactionSource, indication = null) {
                        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                        onClick()
                    }
                } else Modifier
            )
    ) {
        content()
    }
}

/** Hairline-framed square panel with corner ticks — used by the scanner
 *  permission notice and framed illustrations. */
@Composable
fun CornerFrame(
    modifier: Modifier = Modifier,
    frameColor: Color = OffipeColors.Border,
    tickColor: Color = OffipeColors.BorderStrong,
    content: @Composable () -> Unit
) {
    Box(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .drawBehind {
                val w = size.width
                val h = size.height
                drawRect(
                    color = frameColor,
                    topLeft = Offset(0.5f, 0.5f),
                    size = Size(w - 1f, h - 1f),
                    style = Stroke(1f)
                )
                val tick = 14.dp.toPx()
                val stroke = 2.dp.toPx()
                drawLine(tickColor, Offset(0f, 0f), Offset(tick, 0f), stroke, StrokeCap.Butt)
                drawLine(tickColor, Offset(0f, 0f), Offset(0f, tick), stroke, StrokeCap.Butt)
                drawLine(tickColor, Offset(w, 0f), Offset(w - tick, 0f), stroke, StrokeCap.Butt)
                drawLine(tickColor, Offset(w, 0f), Offset(w, tick), stroke, StrokeCap.Butt)
                drawLine(tickColor, Offset(0f, h), Offset(tick, h), stroke, StrokeCap.Butt)
                drawLine(tickColor, Offset(0f, h), Offset(0f, h - tick), stroke, StrokeCap.Butt)
                drawLine(tickColor, Offset(w, h), Offset(w - tick, h), stroke, StrokeCap.Butt)
                drawLine(tickColor, Offset(w, h), Offset(w, h - tick), stroke, StrokeCap.Butt)
            }
    ) {
        content()
    }
}

/** Very subtle diagonal wave texture — the splash / onboarding backdrop. */
@Composable
fun WaveMotif(modifier: Modifier = Modifier, strength: Float = 1f) {
    androidx.compose.foundation.Canvas(modifier) {
        val w = size.width
        val h = size.height
        fun band(x: Float, y: Float): androidx.compose.ui.graphics.Path {
            val p = androidx.compose.ui.graphics.Path()
            p.moveTo(0f, 0f)
            p.lineTo(0f, h * y)
            p.cubicTo(
                w * x * 0.35f, h * y * 0.72f,
                w * x * 0.55f, h * y * 0.28f,
                w * x, 0f
            )
            p.close()
            return p
        }
        drawPath(band(1.15f, 0.34f), OffipeColors.WaveMid.copy(alpha = 0.16f * strength))
        drawPath(band(0.82f, 0.60f), OffipeColors.WaveSoft.copy(alpha = 0.10f * strength))
        drawPath(band(0.50f, 0.92f), OffipeColors.WaveLight.copy(alpha = 0.06f * strength))
    }
}

// ─── Inline alert ─────────────────────────────────────────────────────────────

enum class AlertTone { Info, Warn, Danger }

/**
 * Inline notice — rounded surface card with a status dot, title, body
 * copy and an optional pill action (used for permission prompts).
 */
@Composable
fun AlertStrip(
    title: String,
    message: String,
    tone: AlertTone,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    val accent = when (tone) {
        AlertTone.Info -> OffipeColors.TextSecondary
        AlertTone.Warn -> OffipeColors.Warn
        AlertTone.Danger -> OffipeColors.Danger
    }
    PanelCard(
        modifier = modifier.fillMaxWidth(),
        borderColor = accent.copy(alpha = 0.35f),
        fillColor = OffipeColors.Surface
    ) {
        Row(
            Modifier.padding(start = 14.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LedDot(size = 7.dp, color = accent, pulse = tone != AlertTone.Info)
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = OffipeType.LabelLarge,
                    color = OffipeColors.TextPrimary
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = message,
                    style = OffipeType.BodySmall,
                    color = OffipeColors.TextSecondary
                )
            }
            if (actionLabel != null && onAction != null) {
                Spacer(Modifier.width(10.dp))
                Chip(
                    text = actionLabel,
                    color = accent,
                    onClick = onAction,
                    modifier = Modifier.align(Alignment.CenterVertically)
                )
            }
        }
    }
}

// ─── Segmented control ────────────────────────────────────────────────────────

/**
 * Pill segmented selector — inactive segments sit on the canvas, the
 * active segment inverts to a white pill with black text.
 */
@Composable
fun Segmented(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    Row(
        modifier
            .clip(RoundedCornerShape(50))
            .background(OffipeColors.Surface)
            .border(1.dp, OffipeColors.Border, RoundedCornerShape(50))
            .padding(4.dp)
            .height(36.dp)
    ) {
        options.forEachIndexed { i, option ->
            val selected = i == selectedIndex
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .height(28.dp)
                    .clip(RoundedCornerShape(50))
                    .background(if (selected) OffipeColors.Mark else Color.Transparent)
                    .clickable {
                        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                        if (!selected) onSelect(i)
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = option,
                    style = OffipeType.LabelMedium,
                    color = if (selected) OffipeColors.Black else OffipeColors.TextSecondary
                )
            }
        }
    }
}

// ─── Row primitives ───────────────────────────────────────────────────────────

/**
 * Settings / control-center row: optional leading icon, label + optional
 * sublabel, value right, optional chevron — separated by hairlines.
 */
@Composable
fun ReadoutRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = OffipeColors.TextPrimary,
    sublabel: String? = null,
    leading: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null
) {
    val view = LocalView.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    Row(
        modifier
            .fillMaxWidth()
            .background(if (isPressed && onClick != null) OffipeColors.SurfaceHigh else Color.Transparent)
            .then(
                if (onClick != null) {
                    Modifier.clickable(interactionSource = interactionSource, indication = null) {
                        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                        onClick()
                    }
                } else Modifier
            )
            .padding(vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leading != null) {
            leading()
            Spacer(Modifier.width(13.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(
                text = label,
                style = OffipeType.TitleMedium,
                color = OffipeColors.TextPrimary
            )
            if (sublabel != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = sublabel,
                    style = OffipeType.BodySmall,
                    color = OffipeColors.TextMuted
                )
            }
        }
        if (value.isNotEmpty()) {
            Spacer(Modifier.width(8.dp))
            Text(
                text = value,
                style = OffipeType.LabelMedium,
                color = valueColor
            )
        }
        if (trailing != null) {
            Spacer(Modifier.width(8.dp))
            trailing()
        }
    }
}

/**
 * Circular icon button — the reference's top-bar and action-row key:
 * an exact [size] circle whose hitbox is the circle itself.
 * [label] optionally renders a micro caption beneath the icon.
 */
@Composable
fun IconKey(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    tint: Color = OffipeColors.TextPrimary,
    label: String? = null,
    filled: Boolean = false
) {
    val view = LocalView.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val face = when {
        isPressed -> OffipeColors.SurfaceHigher
        filled -> OffipeColors.SurfaceHigh
        else -> Color.Transparent
    }

    if (label != null) {
        // Labeled key: icon over a caption — hitbox covers the whole chip.
        Column(
            modifier
                .clip(RoundedCornerShape(12.dp))
                .background(face)
                .clickable(interactionSource = interactionSource, indication = null) {
                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    onClick()
                }
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = tint,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = label,
                style = OffipeType.LabelSmall,
                color = tint,
                textAlign = TextAlign.Center
            )
        }
    } else {
        // The hitbox is at least the 44dp minimum even when the drawn
        // circle is smaller (40–42dp call sites): the clickable lives on
        // the outer box, the circle is purely visual inside it.
        Box(
            modifier
                .defaultMinSize(minWidth = 44.dp, minHeight = 44.dp)
                .clickable(interactionSource = interactionSource, indication = null) {
                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    onClick()
                },
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier
                    .size(size)
                    .clip(CircleShape)
                    .background(face)
                    .border(
                        width = 1.dp,
                        color = if (isPressed) OffipeColors.BorderStrong else OffipeColors.Border,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = contentDescription,
                    tint = tint,
                    modifier = Modifier.size((size.value * 0.46f).dp.coerceAtLeast(16.dp))
                )
            }
        }
    }
}

// ─── Transient snackbar ───────────────────────────────────────────────────────

/** Bottom snackbar — rounded surface pill, auto-dismiss. */
@Composable
fun SnackbarStrip(
    message: String?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(message) {
        if (message != null) {
            delay(2_500)
            onDismiss()
        }
    }
    androidx.compose.animation.AnimatedVisibility(
        visible = message != null,
        enter = slideInVertically { it } + fadeIn(),
        exit = slideOutVertically { it } + fadeOut(),
        modifier = modifier
    ) {
        if (message != null) {
            Box(
                Modifier
                    .padding(horizontal = 20.dp)
                    .clip(RoundedCornerShape(50))
                    .background(OffipeColors.SurfaceRaised)
                    .border(1.dp, OffipeColors.Border, RoundedCornerShape(50))
                    .padding(horizontal = 18.dp, vertical = 13.dp)
            ) {
                Text(
                    text = message,
                    style = OffipeType.BodyMedium,
                    color = OffipeColors.TextPrimary
                )
            }
        }
    }
}

// ─── Field ────────────────────────────────────────────────────────────────────

/**
 * Rounded box field — small uppercase label on top, input beneath, thin
 * border that carries focus/error state (grey → white → pastel red),
 * optional trailing slot (e.g. the scan key).
 */
@Composable
fun InlineField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    error: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Done,
    onFocusChange: (Boolean) -> Unit = {},
    trailing: (@Composable () -> Unit)? = null
) {
    var focused by remember { mutableStateOf(false) }
    val borderColor = when {
        error != null -> OffipeColors.Danger
        focused -> OffipeColors.Mark
        else -> OffipeColors.Border
    }
    Column(modifier) {
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(OffipeColors.Surface)
                .border(1.dp, borderColor, RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 11.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = label,
                        style = OffipeType.LabelSmall,
                        color = when {
                            error != null -> OffipeColors.Danger
                            focused -> OffipeColors.TextSecondary
                            else -> OffipeColors.TextMuted
                        }
                    )
                    Spacer(Modifier.height(5.dp))
                    Box {
                        if (value.isEmpty()) {
                            Text(
                                text = placeholder,
                                style = OffipeType.TitleMedium.copy(color = OffipeColors.TextMuted)
                            )
                        }
                        BasicTextField(
                            value = value,
                            onValueChange = onValueChange,
                            textStyle = OffipeType.TitleMedium.copy(color = OffipeColors.TextPrimary),
                            cursorBrush = SolidColor(OffipeColors.Mark),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = keyboardType,
                                imeAction = imeAction
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 20.dp)
                                .onFocusChanged {
                                    focused = it.hasFocus
                                    onFocusChange(it.hasFocus)
                                }
                        )
                    }
                }
                if (trailing != null) {
                    Spacer(Modifier.width(10.dp))
                    trailing()
                }
            }
        }
        if (error != null) {
            Spacer(Modifier.height(7.dp))
            Text(text = error, style = OffipeType.BodySmall, color = OffipeColors.Danger)
        }
    }
}
