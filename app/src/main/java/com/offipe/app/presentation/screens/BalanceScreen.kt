package com.offipe.app.presentation.screens

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import com.offipe.app.data.TransactionEntity
import com.offipe.app.domain.OperationMode
import com.offipe.app.domain.SessionState
import com.offipe.app.presentation.BalanceViewModel
import com.offipe.app.presentation.HistoryViewModel
import com.offipe.app.presentation.ui.components.AlertStrip
import com.offipe.app.presentation.ui.components.AlertTone
import com.offipe.app.presentation.ui.components.Hairline
import com.offipe.app.presentation.ui.components.IconKey
import com.offipe.app.presentation.ui.components.PanelCard
import com.offipe.app.presentation.ui.components.PinGate
import com.offipe.app.presentation.ui.components.PrimaryActionBar
import com.offipe.app.presentation.ui.components.SnackbarStrip
import com.offipe.app.presentation.ui.components.Tag
import com.offipe.app.presentation.ui.theme.OffipeColors
import com.offipe.app.presentation.ui.theme.OffipeType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay

/**
 * BALANCE — reference layout:
 *  1. Title block (`Balance` + `OFFLINE UPI · *99#` overline).
 *  2. Balance card: CURRENT BALANCE label, figure, refresh key, and
 *     "last checked" line.
 *  3. Three circle actions: Check Balance / History / Pay.
 *  4. Recent Activity list with SEE ALL.
 *  5. Docked pill for the check itself (opens the [PinGate]; auto-fire
 *     runs `attemptCheckBalance()` at full PIN length). Sessions resolve
 *     in the scaffold's SessionOverlay.
 */
@Composable
fun BalanceScreen(
    viewModel: BalanceViewModel,
    historyViewModel: HistoryViewModel,
    onOpenHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ui by viewModel.uiState.collectAsState()
    val session by viewModel.sessionState.collectAsState()
    val mode by viewModel.operationMode.collectAsState()
    val last by viewModel.lastResult.collectAsState()
    val snackbar by viewModel.snackbar.collectAsState()
    val txns by historyViewModel.transactions.collectAsState()
    val pinLength by viewModel.pinLength.collectAsState(initial = 6)

    var gateOpen by remember { mutableStateOf(false) }

    LaunchedEffect(session) {
        if (session !is SessionState.Idle) gateOpen = false
    }

    // Auto-fire when the configured number of digits is in (debounced 250ms).
    LaunchedEffect(ui.pin, gateOpen) {
        if (gateOpen && ui.pin.length == pinLength && mode != OperationMode.MANUAL) {
            delay(250)
            viewModel.attemptCheckBalance()
        }
    }

    fun startCheck() {
        if (mode == OperationMode.MANUAL) {
            viewModel.attemptCheckBalance()
        } else {
            viewModel.onPinChanged("")
            gateOpen = true
        }
    }

    Box(
        modifier
            .fillMaxSize()
            .background(OffipeColors.Black)
    ) {
        Column(
            modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
        ) {
            // ── Title block ──
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Balance",
                        style = OffipeType.DisplayMedium,
                        color = OffipeColors.TextPrimary
                    )
                    Spacer(Modifier.height(3.dp))
                    Tag("Offline UPI · *99#", color = OffipeColors.TextSecondary)
                }
                IconKey(
                    icon = Icons.Filled.History,
                    contentDescription = "History",
                    onClick = onOpenHistory,
                    size = 42.dp,
                    tint = OffipeColors.TextSecondary
                )
            }

            Column(Modifier.padding(horizontal = 20.dp)) {
                Spacer(Modifier.height(6.dp))

                // ── Balance card ──
                PanelCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Current Balance",
                                style = OffipeType.LabelMedium,
                                color = OffipeColors.TextSecondary,
                                modifier = Modifier.weight(1f)
                            )
                            IconKey(
                                icon = Icons.Default.Refresh,
                                contentDescription = "Check balance",
                                onClick = ::startCheck,
                                size = 40.dp,
                                tint = OffipeColors.TextPrimary
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = last?.let { parseBalance(it.text) } ?: "₹ —",
                            style = OffipeType.AmountBalance.copy(
                                color = if (last != null) OffipeColors.TextPrimary
                                else OffipeColors.TextMuted
                            )
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = last?.let {
                                "Last checked ${formatTimestamp(it.timestamp)}"
                            } ?: "No check yet",
                            style = OffipeType.BodySmall,
                            color = OffipeColors.TextMuted
                        )
                    }
                }

                if (mode == OperationMode.MANUAL) {
                    Spacer(Modifier.height(12.dp))
                    AlertStrip(
                        title = "Manual mode",
                        message = "The button below opens the dialer with *99*3# ready. " +
                            "Enter your PIN in the dialer.",
                        tone = AlertTone.Info,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(Modifier.height(16.dp))

                // ── Circle actions ──
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    CircleAction(
                        icon = Icons.Default.Refresh,
                        label = "Check Balance",
                        onClick = ::startCheck
                    )
                    CircleAction(
                        icon = Icons.AutoMirrored.Filled.ReceiptLong,
                        label = "History",
                        onClick = onOpenHistory
                    )
                    CircleAction(
                        icon = Icons.Default.History,
                        label = "Latest",
                        onClick = onOpenHistory,
                        enabled = txns.isNotEmpty()
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
                        ) { onOpenHistory() }
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
                        CompactRow(txn)
                        Hairline()
                    }
                }

                Spacer(Modifier.height(16.dp))
            }

            // ── Docked pill ──
            Box(Modifier.padding(horizontal = 20.dp, vertical = 10.dp)) {
                PrimaryActionBar(
                    text = when (mode) {
                        OperationMode.MANUAL -> "Open Dialer"
                        else -> "Check Balance"
                    },
                    onClick = ::startCheck,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        if (gateOpen && mode != OperationMode.MANUAL) {
            PinGate(
                title = "Authorize balance check",
                statement = "₹ ?",
                hint = "AVAILABLE BALANCE  ·  $pinLength DIGITS",
                pin = ui.pin,
                pinLength = pinLength,
                onPinChange = viewModel::onPinChanged,
                onDismiss = {
                    gateOpen = false
                    viewModel.onPinChanged("")
                },
                error = ui.pinError
            )
        }

        SnackbarStrip(
            message = snackbar,
            onDismiss = viewModel::dismissSnackbar,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 96.dp)
        )
    }
}

// ─── Pieces ───────────────────────────────────────────────────────────────────

@Composable
private fun CircleAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    val view = LocalView.current
    val interactionSource = remember { MutableInteractionSource() }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
            .clickable(interactionSource = interactionSource, indication = null) {
                if (enabled) {
                    view.performHapticFeedback(
                        android.view.HapticFeedbackConstants.VIRTUAL_KEY
                    )
                    onClick()
                }
            }
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Box(
            Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(OffipeColors.SurfaceHigh),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (enabled) OffipeColors.TextPrimary else OffipeColors.TextMuted,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(Modifier.height(7.dp))
        Text(
            text = label,
            style = OffipeType.BodySmall,
            color = if (enabled) OffipeColors.TextSecondary else OffipeColors.TextMuted
        )
    }
}

@Composable
private fun CompactRow(txn: TransactionEntity) {
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
                text = formatTimestamp(txn.timestamp),
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

// ─── Parsing helpers ──────────────────────────────────────────────────────────

private val BALANCE_REGEX =
    Regex("""(?:rs\.?|inr|₹)\s*([\d,]+(?:\.\d{1,2})?)""", RegexOption.IGNORE_CASE)

/**
 * Best-effort balance amount extraction from the carrier's reply text.
 * Looks for the first "₹ X,XXX.XX" / "Rs 12345.67" / "INR 100" pattern.
 */
private fun parseBalance(text: String): String? {
    val match = BALANCE_REGEX.find(text) ?: return null
    val rawAmount = match.groupValues[1]
    return "₹ $rawAmount"
}

private fun formatTimestamp(ts: Long): String {
    val now = System.currentTimeMillis()
    val delta = now - ts
    if (delta < 60_000) return "just now"
    if (delta < 3_600_000) return "${delta / 60_000} min ago"
    if (delta < 86_400_000) return "${delta / 3_600_000} hours ago"
    if (delta < 172_800_000) return "yesterday"
    val sdf = SimpleDateFormat("d MMM", Locale.getDefault())
    return sdf.format(Date(ts))
}
