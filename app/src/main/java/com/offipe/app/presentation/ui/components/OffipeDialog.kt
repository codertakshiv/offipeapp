package com.offipe.app.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.offipe.app.presentation.ui.theme.OffipeColors
import com.offipe.app.presentation.ui.theme.OffipeType

/**
 * Offipe dialog — a rounded surface card on the scrim.
 *
 * Title in semibold, body in secondary grey, and the same pill action
 * language as every screen: ghost cancel + primary (or danger) commit.
 */
@Composable
fun OffipeDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    dismissLabel: String = "Cancel",
    danger: Boolean = false
) {
    Dialog(onDismissRequest = onDismiss) {
        OffipeDialogSurface(modifier = modifier) {
            Column(Modifier.padding(horizontal = 22.dp, vertical = 22.dp)) {
                Text(
                    text = title,
                    style = OffipeType.TitleLarge,
                    color = if (danger) OffipeColors.Danger else OffipeColors.TextPrimary,
                    textAlign = TextAlign.Start
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = message,
                    style = OffipeType.BodyMedium,
                    color = OffipeColors.TextSecondary
                )
                Spacer(Modifier.height(22.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GhostActionBar(
                        text = dismissLabel,
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        height = 48.dp
                    )
                    if (danger) {
                        GhostActionBar(
                            text = confirmLabel,
                            onClick = onConfirm,
                            modifier = Modifier.weight(1f),
                            danger = true,
                            height = 48.dp
                        )
                    } else {
                        PrimaryActionBar(
                            text = confirmLabel,
                            onClick = onConfirm,
                            modifier = Modifier.weight(1f),
                            height = 48.dp
                        )
                    }
                }
            }
        }
    }
}

/**
 * Base panel used by [OffipeDialog] and bespoke prompts — rounded face,
 * thin border.
 */
@Composable
fun OffipeDialogSurface(
    modifier: Modifier = Modifier,
    contentPadding: androidx.compose.foundation.layout.PaddingValues =
        androidx.compose.foundation.layout.PaddingValues(0.dp),
    content: @Composable () -> Unit
) {
    Column(
        modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(20.dp))
            .background(OffipeColors.SurfaceRaised)
            .border(1.dp, OffipeColors.Border, androidx.compose.foundation.shape.RoundedCornerShape(20.dp))
            .padding(contentPadding)
    ) {
        content()
    }
}

/** Renders [text] as a muted note inside any dialog surface. */
@Composable
fun DialogNote(text: String, color: Color = OffipeColors.TextMuted) {
    Text(
        text = text,
        style = OffipeType.BodySmall,
        color = color
    )
}
