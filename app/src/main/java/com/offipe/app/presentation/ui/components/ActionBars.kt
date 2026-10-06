package com.offipe.app.presentation.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.offipe.app.presentation.ui.theme.OffipeColors
import com.offipe.app.presentation.ui.theme.OffipeType

/**
 * The Offipe action language — PILL BUTTONS.
 *
 *  - [PrimaryActionBar] — solid white pill, black label, arrow at the
 *    right edge. The one commit action per screen.
 *  - [GhostActionBar]    — outlined pill; [danger] renders the outline
 *    and label in pastel red for destructive / abort intent.
 *  - Bars sit flush in rows for split actions (WHY | RETRY).
 *
 * The label is sentence case (not tracked mono) and the whole bar is the
 * touch target.
 */

private enum class BarFace { Primary, Ghost, Danger }

@Composable
fun PrimaryActionBar(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    value: String? = null,
    height: Dp = 54.dp,
    icon: ImageVector = Icons.AutoMirrored.Filled.ArrowForward
) {
    ActionBarFace(
        face = BarFace.Primary,
        text = text,
        value = value,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        height = height,
        icon = icon
    )
}

@Composable
fun GhostActionBar(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    danger: Boolean = false,
    value: String? = null,
    height: Dp = 54.dp,
    icon: ImageVector = Icons.AutoMirrored.Filled.ArrowForward
) {
    ActionBarFace(
        face = if (danger) BarFace.Danger else BarFace.Ghost,
        text = text,
        value = value,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        height = height,
        icon = icon
    )
}

@Composable
private fun ActionBarFace(
    face: BarFace,
    text: String,
    value: String?,
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    height: Dp,
    icon: ImageVector
) {
    val view = LocalView.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val faceColor = when {
        !enabled -> OffipeColors.Surface
        face == BarFace.Primary ->
            if (isPressed) OffipeColors.AccentDim else OffipeColors.Mark
        isPressed -> OffipeColors.SurfaceHigher
        else -> Color.Transparent
    }
    val labelColor = when {
        !enabled -> OffipeColors.TextMuted
        face == BarFace.Primary -> OffipeColors.Black
        face == BarFace.Danger -> OffipeColors.Danger
        else -> OffipeColors.TextPrimary
    }
    val borderColor = when {
        !enabled -> OffipeColors.Border
        face == BarFace.Primary -> Color.Transparent
        face == BarFace.Danger -> OffipeColors.Danger.copy(alpha = 0.7f)
        else -> OffipeColors.BorderStrong
    }
    val arrowColor = when {
        !enabled -> OffipeColors.TextMuted
        face == BarFace.Primary -> OffipeColors.Black
        face == BarFace.Danger -> OffipeColors.Danger
        else -> OffipeColors.TextPrimary
    }

    Row(
        modifier
            .height(height)
            .clip(RoundedCornerShape(50))
            .background(faceColor)
            .border(1.dp, borderColor, RoundedCornerShape(50))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled
            ) {
                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                onClick()
            }
            .padding(start = 22.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.weight(1f).fillMaxHeight(),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = text,
                    style = OffipeType.LabelLarge,
                    color = labelColor
                )
                if (value != null) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = value,
                        style = OffipeType.LabelLarge,
                        color = if (face == BarFace.Primary) OffipeColors.Black
                        else OffipeColors.TextSecondary
                    )
                }
            }
        }
        Box(
            Modifier
                .size(height.minus(8.dp).coerceAtLeast(36.dp))
                .graphicsLayer {
                    val s = if (isPressed && enabled) 0.88f else 1f
                    scaleX = s
                    scaleY = s
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = arrowColor,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Small outlined key — inline secondary commands (SKIP, RECOVER, COPY).
 * Pill face; the whole chip is the touch target.
 */
@Composable
fun TextKey(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = OffipeColors.TextSecondary
) {
    val view = LocalView.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    Box(
        modifier
            // Material minimum touch target, whatever the label width is.
            .defaultMinSize(minWidth = 44.dp, minHeight = 44.dp)
            .clip(RoundedCornerShape(50))
            .background(if (isPressed) color.copy(alpha = 0.12f) else Color.Transparent)
            .border(
                width = 1.dp,
                color = color.copy(alpha = if (isPressed) 0.9f else 0.55f),
                shape = RoundedCornerShape(50)
            )
            .clickable(interactionSource = interactionSource, indication = null) {
                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                onClick()
            }
            .padding(horizontal = 15.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = OffipeType.LabelMedium,
            color = color
        )
    }
}
