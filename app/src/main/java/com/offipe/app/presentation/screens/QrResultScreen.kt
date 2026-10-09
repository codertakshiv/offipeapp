package com.offipe.app.presentation.screens

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.offipe.app.R
import com.offipe.app.domain.UpiParser
import com.offipe.app.presentation.PayViewModel
import com.offipe.app.presentation.ui.components.GhostActionBar
import com.offipe.app.presentation.ui.components.IconKey
import com.offipe.app.presentation.ui.components.PanelCard
import com.offipe.app.presentation.ui.components.PrimaryActionBar
import com.offipe.app.presentation.ui.components.Tag
import com.offipe.app.presentation.ui.theme.OffipeColors
import com.offipe.app.presentation.ui.theme.OffipeType

/**
 * QR RESULT — what the scan found, before anything is typed.
 *
 * Parses the raw payload locally: valid UPI → avatar + payee + amount +
 * note with CONTINUE TO PAY (which pushes the parsed values into the Pay
 * form); anything else → a clear "not a UPI QR" state with SCAN AGAIN.
 * Nothing moves money from here.
 */
@Composable
fun QrResultScreen(
    raw: String?,
    payViewModel: PayViewModel,
    onContinueToPay: () -> Unit,
    onBack: () -> Unit,
    onScanAgain: () -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val upiData = remember(raw) { raw?.let { UpiParser.parse(it) } }

    Column(
        modifier
            .fillMaxSize()
            .background(OffipeColors.Black)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // ── Top bar ──
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconKey(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                onClick = onBack,
                size = 42.dp,
                tint = OffipeColors.TextSecondary
            )
            Spacer(Modifier.weight(1f))
            if (upiData != null) {
                Tag("Scanned · UPI QR", color = OffipeColors.TextSecondary)
            }
            Spacer(Modifier.weight(1f))
            Box(Modifier.size(42.dp))
        }

        if (raw == null) {
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = stringResource(R.string.qr_reading),
                    style = OffipeType.TitleLarge,
                    color = OffipeColors.TextPrimary
                )
            }
            return@Column
        }

        if (upiData == null) {
            NotUpiState(onScanAgain = onScanAgain)
            return@Column
        }

        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Avatar with payee initial
            Box(
                Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(OffipeColors.SurfaceHigh),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = (upiData.payeeName ?: upiData.vpa)
                        .firstOrNull()
                        ?.uppercaseChar()
                        ?.toString()
                        ?: "?",
                    style = OffipeType.DisplaySmall,
                    color = OffipeColors.TextPrimary
                )
            }
            Spacer(Modifier.height(18.dp))
            Text(
                text = upiData.vpa,
                style = OffipeType.HeadlineLarge,
                color = OffipeColors.TextPrimary
            )
            upiData.payeeName?.let {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = it,
                    style = OffipeType.BodyMedium,
                    color = OffipeColors.TextSecondary
                )
            }

            Spacer(Modifier.height(26.dp))

            PanelCard(Modifier.fillMaxWidth()) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (upiData.amount != null) {
                        Tag("Amount")
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "₹${upiData.amount}",
                            style = OffipeType.AmountLg,
                            color = OffipeColors.TextPrimary
                        )
                    } else {
                        Tag("No amount requested")
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "You can enter the amount before paying.",
                            style = OffipeType.BodyMedium,
                            color = OffipeColors.TextSecondary
                        )
                    }
                    if (!upiData.transactionNote.isNullOrBlank()) {
                        Spacer(Modifier.height(14.dp))
                        Tag("Note")
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = upiData.transactionNote,
                            style = OffipeType.BodyMedium,
                            color = OffipeColors.TextSecondary
                        )
                    }
                }
            }
        }

        // ── Actions ──
        Column(Modifier.padding(horizontal = 20.dp)) {
            PrimaryActionBar(
                text = "Continue to Pay",
                onClick = {
                    view.performHapticFeedback(
                        android.view.HapticFeedbackConstants.VIRTUAL_KEY
                    )
                    raw?.let { payViewModel.onQrScanned(it) }
                    onContinueToPay()
                },
                icon = Icons.Default.ArrowForward,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            GhostActionBar(
                text = "Scan again",
                onClick = onScanAgain,
                icon = Icons.Default.QrCodeScanner,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun NotUpiState(onScanAgain: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.QrCodeScanner,
            contentDescription = null,
            tint = OffipeColors.TextMuted,
            modifier = Modifier.size(40.dp)
        )
        Spacer(Modifier.height(18.dp))
        Text(
            text = "Not a UPI QR code",
            style = OffipeType.DisplaySmall,
            color = OffipeColors.TextPrimary
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Offipe reads upi://pay QR codes. Scan the code shown " +
                "at the counter or in the payer's app.",
            style = OffipeType.BodyMedium,
            color = OffipeColors.TextSecondary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(Modifier.height(26.dp))
        PrimaryActionBar(
            text = "Scan again",
            onClick = onScanAgain,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
