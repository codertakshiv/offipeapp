package com.offipe.app.presentation.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.offipe.app.presentation.ui.theme.OffipeColors
import com.offipe.app.presentation.ui.theme.OffipeType

/**
 * The app's numeric pad — PAY amount entry and PIN entry share this one.
 *
 * Reference layout: 4×3 grid of rounded dark keys with breathing room
 * between them. Each key fills its entire grid cell, so the touch target
 * is always the visible key face — never a text-sized sliver.
 */
@Composable
fun NumericPad(
    onDigit: (Char) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    onClear: (() -> Unit)? = null,
    showDecimal: Boolean = false,
    rowHeight: Dp = 58.dp
) {
    Column(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9")
        ).forEach { row ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(rowHeight),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { d ->
                    PadKey(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        onClick = { onDigit(d[0]) }
                    ) {
                        Text(d, style = OffipeType.KeyDigit, color = OffipeColors.TextPrimary)
                    }
                }
            }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .height(rowHeight),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (showDecimal) {
                PadKey(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    onClick = { onDigit('.') }
                ) {
                    Text(".", style = OffipeType.KeyDigit, color = OffipeColors.TextPrimary)
                }
            } else {
                PadKey(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    onClick = { onClear?.invoke() },
                    enabled = onClear != null
                ) {
                    Text(
                        "CLR",
                        style = OffipeType.LabelSmall,
                        color = if (onClear != null) OffipeColors.TextSecondary
                        else OffipeColors.TextMuted
                    )
                }
            }
            PadKey(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                onClick = { onDigit('0') }
            ) {
                Text("0", style = OffipeType.KeyDigit, color = OffipeColors.TextPrimary)
            }
            PadKey(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                onClick = { onDelete() }
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Backspace,
                    contentDescription = "Delete",
                    tint = OffipeColors.TextPrimary,
                    modifier = Modifier.height(22.dp)
                )
            }
        }
    }
}

/**
 * One key cell. The clickable surface is the whole cell ([modifier]
 * carries weight + fillMaxHeight from the caller), rendered as a rounded
 * key face inset by nothing — hitbox == visible face.
 */
@Composable
private fun PadKey(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val face = when {
        !enabled -> Color.Transparent
        isPressed -> OffipeColors.SurfaceRaised
        else -> OffipeColors.SurfaceHigh
    }
    Box(
        modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(12.dp))
            .background(face)
            .border(
                width = 1.dp,
                color = if (isPressed && enabled) OffipeColors.BorderStrong
                else OffipeColors.Border,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled
            ) {
                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
