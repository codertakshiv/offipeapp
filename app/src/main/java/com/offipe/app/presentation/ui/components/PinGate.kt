package com.offipe.app.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.offipe.app.presentation.ui.theme.OffipeColors
import com.offipe.app.presentation.ui.theme.OffipeType

/**
 * PIN gate — a full-screen authorization takeover, not a card on a form.
 *
 * Structure (top to bottom):
 *  1. Title row with an ESC key.
 *  2. The statement being authorized — amount in hero type, payee hint
 *     beneath.
 *  3. PIN readout — round dots (width-independent, so 4- and 6-digit
 *     PINs both center cleanly).
 *  4. The numeric pad, anchored to the bottom edge.
 *
 * The caller owns auto-fire: when `pin.length == pinLength` it submits
 * (debounced), exactly as before — this component only edits the string.
 */
@Composable
fun PinGate(
    title: String,
    pin: String,
    pinLength: Int,
    onPinChange: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    statement: String? = null,
    hint: String? = null,
    error: String? = null,
    useBoxes: Boolean = false
) {
    Box(
        modifier
            .fillMaxSize()
            .background(OffipeColors.Black)
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Title row
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = OffipeType.TitleLarge,
                    color = OffipeColors.TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                IconKey(
                    icon = Icons.Filled.Close,
                    contentDescription = "Cancel",
                    onClick = onDismiss,
                    size = 42.dp,
                    tint = OffipeColors.TextSecondary
                )
            }

            Spacer(Modifier.height(20.dp))

            if (statement != null) {
                Text(
                    text = statement,
                    style = OffipeType.AmountHero.copy(color = OffipeColors.TextPrimary),
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
                Spacer(Modifier.height(8.dp))
            }
            Text(
                text = hint ?: "ENTER UPI PIN",
                style = OffipeType.TerminalLabel,
                color = if (error != null) OffipeColors.Danger else OffipeColors.TextSecondary,
                modifier = Modifier.padding(horizontal = 20.dp)
            )

            Spacer(Modifier.height(28.dp))

            if (useBoxes) {
                PinBoxes(pin = pin, pinLength = pinLength, error = error)
            } else {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(pinLength) { i ->
                        val filled = i < pin.length
                        Box(
                            Modifier
                                .size(if (filled) 16.dp else 12.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        filled -> OffipeColors.Mark
                                        error != null -> OffipeColors.Danger
                                        else -> OffipeColors.BorderStrong
                                    }
                                )
                        )
                    }
                }
            }

            if (error != null) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = error,
                    style = OffipeType.BodyMedium,
                    color = OffipeColors.Danger,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.weight(1f))

            NumericPad(
                onDigit = { d ->
                    if (pin.length < pinLength) {
                        val next = pin + d
                        onPinChange(next)
                    }
                },
                onDelete = {
                    if (pin.isNotEmpty()) onPinChange(pin.dropLast(1))
                },
                onClear = {
                    if (pin.isNotEmpty()) onPinChange("")
                },
                showDecimal = false
            )
        }
    }
}

@Composable
fun PinBoxes(
    pin: String,
    pinLength: Int,
    error: String? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(pinLength) { index ->
            Box(
                Modifier
                    .weight(1f)
                    .widthIn(max = 42.dp)
                    .height(48.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(OffipeColors.Surface)
                    .border(
                        width = 1.dp,
                        color = when {
                            error != null -> OffipeColors.Danger
                            index < pin.length -> OffipeColors.Mark
                            else -> OffipeColors.BorderStrong
                        },
                        shape = RoundedCornerShape(6.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (index < pin.length) {
                    Box(
                        Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(OffipeColors.Mark)
                    )
                }
            }
        }
    }
}

/** Small helper for squared icon plates used across overlays. */
@Composable
fun PlateBox(
    size: androidx.compose.ui.unit.Dp,
    color: Color,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier
            .size(size)
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(10.dp))
            .background(color),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
