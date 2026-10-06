package com.offipe.app.presentation.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import com.offipe.app.R
import com.offipe.app.data.TransactionEntity
import com.offipe.app.presentation.HistoryViewModel
import com.offipe.app.presentation.permissions.PermissionStatus
import com.offipe.app.presentation.ui.components.Hairline
import com.offipe.app.presentation.ui.components.IconKey
import com.offipe.app.presentation.ui.components.PanelCard
import com.offipe.app.presentation.ui.theme.OffipeColors
import com.offipe.app.presentation.ui.theme.OffipeType
import com.offipe.app.presentation.ui.components.offipeLogoPainter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * HOME — the hub.
 *
 * Greeting (local time + name saved during first-run setup) → readiness card (tap = straight into Pay) → 2×2 quick
 * actions → recent activity. Navigation is a push stack from here; the
 * top-right key opens Settings. Tapping the mark 5× is the money-rain
 * easter egg.
 */
@Composable
fun HomeScreen(
    historyViewModel: HistoryViewModel,
    permissions: PermissionStatus,
    onNavigatePay: () -> Unit,
    onNavigateScan: () -> Unit,
    onNavigateBalance: () -> Unit,
    onNavigateHistory: () -> Unit,
    onNavigateSettings: () -> Unit,
    onMarkTap: () -> Unit = {},
    userName: String = "",
    modifier: Modifier = Modifier
) {
    val txns by historyViewModel.transactions.collectAsState()
    val view = LocalView.current
    val markInteraction = remember { MutableInteractionSource() }

    val ready = permissions.readyForDialerPay
    // Real local time — resolved once instead of on every recomposition.
    val greetingText = remember { greeting() }

    Column(
        modifier
            .fillMaxSize()
            .background(OffipeColors.Black)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
    ) {
        // ── Top bar: mark + name, settings key ──
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = offipeLogoPainter(),
                contentDescription = "Offipe",
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .clickable(interactionSource = markInteraction, indication = null) {
                        view.performHapticFeedback(
                            android.view.HapticFeedbackConstants.VIRTUAL_KEY
                        )
                        onMarkTap()
                    }
            )
            Spacer(Modifier.width(11.dp))
            Text(
                text = "Offipe",
                style = OffipeType.HeadlineLarge,
                color = OffipeColors.TextPrimary
            )
            Spacer(Modifier.weight(1f))
            IconKey(
                icon = Icons.Default.Settings,
                contentDescription = "Settings",
                onClick = onNavigateSettings,
                size = 42.dp,
                tint = OffipeColors.TextSecondary
            )
        }

        Column(Modifier.padding(horizontal = 20.dp)) {
            Spacer(Modifier.height(10.dp))

            // ── Greeting: real local time + the name saved at setup ──
            Text(
                text = "$greetingText,",
                style = OffipeType.BodyLarge,
                color = OffipeColors.TextSecondary
            )
            val name = userName.trim()
            if (name.isNotEmpty()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = name,
                    style = OffipeType.DisplayMedium,
                    color = OffipeColors.TextPrimary
                )
            }
            Spacer(Modifier.height(14.dp))

            // ── Readiness card ──
            PanelCard(
                onClick = onNavigatePay,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(OffipeColors.SurfaceHigher),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CurrencyRupee,
                            contentDescription = null,
                            tint = OffipeColors.TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = if (ready) "Ready to pay" else "Finish setup",
                            style = OffipeType.TitleLarge,
                            color = OffipeColors.TextPrimary
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = if (ready) "Even offline" else "Grant permissions to pay",
                            style = OffipeType.BodySmall,
                            color = OffipeColors.TextSecondary
                        )
                    }
                    IconKey(
                        icon = Icons.Default.ArrowForward,
                        contentDescription = "Open Pay",
                        onClick = onNavigatePay,
                        size = 40.dp,
                        tint = OffipeColors.TextPrimary,
                        filled = true
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Quick actions 2×2 ──
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickTile(
                    icon = Icons.Default.CurrencyRupee,
                    label = "Pay",
                    caption = "UPI / *99#",
                    onClick = onNavigatePay,
                    modifier = Modifier.weight(1f)
                )
                QuickTile(
                    icon = Icons.Default.QrCodeScanner,
                    label = "Scan",
                    caption = "QR code",
                    onClick = onNavigateScan,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickTile(
                    icon = Icons.Default.AccountBalance,
                    label = "Balance",
                    caption = "Check now",
                    onClick = onNavigateBalance,
                    modifier = Modifier.weight(1f)
                )
                QuickTile(
                    icon = Icons.AutoMirrored.Filled.ReceiptLong,
                    label = "History",
                    caption = "View all",
                    onClick = onNavigateHistory,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(24.dp))

            // ── Recent activity ──
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Activity",
                    style = OffipeType.TitleLarge,
                    color = OffipeColors.TextPrimary
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = "See all",
                    style = OffipeType.LabelMedium,
                    color = OffipeColors.TextSecondary,
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onNavigateHistory() }
                )
            }
            Spacer(Modifier.height(6.dp))

            if (txns.isEmpty()) {
                Text(
                    text = "Your payments will show up here.",
                    style = OffipeType.BodyMedium,
                    color = OffipeColors.TextMuted,
                    modifier = Modifier.padding(vertical = 14.dp)
                )
            } else {
                txns.take(3).forEach { txn ->
                    CompactTxnRow(txn)
                    Hairline()
                }
            }

            Spacer(Modifier.height(28.dp))
        }
    }
}

// ─── Pieces ───────────────────────────────────────────────────────────────────

@Composable
private fun QuickTile(
    icon: ImageVector,
    label: String,
    caption: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    PanelCard(onClick = onClick, modifier = modifier) {
        Column(Modifier.padding(14.dp)) {
            Box(
                Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(OffipeColors.SurfaceHigher),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = OffipeColors.TextPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(Modifier.height(18.dp))
            Text(
                text = label,
                style = OffipeType.TitleLarge,
                color = OffipeColors.TextPrimary
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = caption,
                style = OffipeType.BodySmall,
                color = OffipeColors.TextMuted
            )
        }
    }
}

@Composable
private fun CompactTxnRow(txn: TransactionEntity) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(OffipeColors.SurfaceHigh),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.History,
                contentDescription = null,
                tint = OffipeColors.TextSecondary,
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = txn.payeeName ?: txn.vpa,
                style = OffipeType.TitleMedium,
                color = OffipeColors.TextPrimary,
                maxLines = 1
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = formatWhen(txn.timestamp),
                style = OffipeType.BodySmall,
                color = OffipeColors.TextMuted
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = "₹${txn.amount}",
            style = OffipeType.AmountMd,
            color = OffipeColors.TextPrimary
        )
    }
}

private fun greeting(): String {
    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        in 17..21 -> "Good evening"
        else -> "Good night"
    }
}

private fun formatWhen(ts: Long): String {
    val now = System.currentTimeMillis()
    val delta = now - ts
    if (delta < 60_000) return "Just now"
    if (delta < 3_600_000) return "${delta / 60_000} min ago"
    if (delta < 86_400_000) return "${delta / 3_600_000} hours ago"
    if (delta < 172_800_000) return "Yesterday"
    val sdf = SimpleDateFormat("d MMM", Locale.getDefault())
    return sdf.format(Date(ts))
}
