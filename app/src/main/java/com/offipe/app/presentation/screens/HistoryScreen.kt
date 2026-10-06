package com.offipe.app.presentation.screens

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.NorthEast
import androidx.compose.material3.Icon
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import com.offipe.app.data.TransactionEntity
import com.offipe.app.presentation.HistoryViewModel
import com.offipe.app.presentation.ui.components.GhostActionBar
import com.offipe.app.presentation.ui.components.Hairline
import com.offipe.app.presentation.ui.components.IconKey
import com.offipe.app.presentation.ui.components.LedDot
import com.offipe.app.presentation.ui.components.OffipeDialog
import com.offipe.app.presentation.ui.components.PrimaryActionBar
import com.offipe.app.presentation.ui.components.Tag
import com.offipe.app.presentation.ui.components.TextKey
import com.offipe.app.presentation.ui.theme.OffipeColors
import com.offipe.app.presentation.ui.theme.OffipeType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * HISTORY — the reference's transaction list.
 *
 * Chronological rows: a pastel-green outgoing chip (payment completed),
 * payee/VPA + relative time, amount right-aligned, hairlines between.
 * Tap expands a row into its carrier reply with PAY AGAIN / DELETE;
 * swipe left or long-press also deletes (confirm dialogs unchanged).
 * CLEAR sits in the header.
 *
 * No Sent/Received filter: every record in this ledger is a payment the
 * user sent, so a second tab would be an empty fake.
 */
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    onPay: () -> Unit,
    onPayAgain: (TransactionEntity) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val txns by viewModel.transactions.collectAsState()
    var showClearAllDialog by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<TransactionEntity?>(null) }

    Column(
        modifier
            .fillMaxSize()
            .background(OffipeColors.Black)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // ── Header ──
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconKey(
                icon = Icons.Default.ArrowBack,
                contentDescription = "Back",
                onClick = onClose,
                size = 42.dp,
                tint = OffipeColors.TextSecondary
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = "History",
                    style = OffipeType.DisplaySmall,
                    color = OffipeColors.TextPrimary
                )
                Spacer(Modifier.height(3.dp))
                Tag(
                    if (txns.isNotEmpty()) {
                        "${txns.size} record${if (txns.size == 1) "" else "s"}"
                    } else "No records",
                    color = OffipeColors.TextSecondary
                )
            }
            if (txns.isNotEmpty()) {
                TextKey(
                    text = "Clear",
                    onClick = { showClearAllDialog = true },
                    color = OffipeColors.Danger
                )
            }
        }

        if (txns.isEmpty()) {
            EmptyState(onPay = onPay, modifier = Modifier.fillMaxSize())
        } else {
            LazyColumn(Modifier.weight(1f)) {
                items(items = txns, key = { it.id }) { txn ->
                    SwipeableLedgerRow(
                        txn = txn,
                        onDelete = { viewModel.deleteTransaction(txn.id) },
                        onLongPress = { pendingDelete = txn },
                        onPayAgain = { onPayAgain(txn) }
                    )
                    Hairline()
                }
                item { Spacer(Modifier.height(28.dp)) }
            }
        }
    }

    if (showClearAllDialog) {
        OffipeDialog(
            title = "Delete all transactions?",
            message = "This will permanently remove every transaction from your " +
                "history. This cannot be undone.",
            confirmLabel = "Delete all",
            onConfirm = {
                viewModel.clearHistory()
                showClearAllDialog = false
            },
            onDismiss = { showClearAllDialog = false },
            danger = true
        )
    }

    pendingDelete?.let { txn ->
        OffipeDialog(
            title = "Delete this transaction?",
            message = "₹${txn.amount} sent to ${txn.vpa}. This cannot be undone.",
            confirmLabel = "Delete",
            onConfirm = {
                viewModel.deleteTransaction(txn.id)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null },
            danger = true
        )
    }
}

// ─── Empty state ──────────────────────────────────────────────────────────────

@Composable
private fun EmptyState(onPay: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.Center) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(OffipeColors.SurfaceHigh),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.NorthEast,
                    contentDescription = null,
                    tint = OffipeColors.TextMuted,
                    modifier = Modifier.size(26.dp)
                )
            }
            Spacer(Modifier.height(18.dp))
            Text(
                text = "No payments yet",
                style = OffipeType.DisplaySmall,
                color = OffipeColors.TextPrimary
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Your payments will show up here.",
                style = OffipeType.BodyMedium,
                color = OffipeColors.TextMuted
            )
        }
        Spacer(Modifier.weight(1f))
        PrimaryActionBar(
            text = "Make a payment",
            onClick = onPay,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// ─── Ledger rows ──────────────────────────────────────────────────────────────

@Composable
private fun SwipeableLedgerRow(
    txn: TransactionEntity,
    onDelete: () -> Unit,
    onLongPress: () -> Unit,
    onPayAgain: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            // Only allow EndToStart (right→left swipe) to trigger delete.
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                true
            } else false
        },
        positionalThreshold = { totalDistance -> totalDistance * 0.5f }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(OffipeColors.SurfaceHigh)
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = OffipeColors.Danger,
                    modifier = Modifier.size(20.dp)
                )
            }
        },
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true
    ) {
        LedgerRow(
            txn = txn,
            onLongPress = onLongPress,
            onPayAgain = onPayAgain,
            onDelete = onDelete
        )
    }
}

@Composable
private fun LedgerRow(
    txn: TransactionEntity,
    onLongPress: () -> Unit,
    onPayAgain: () -> Unit,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val view = LocalView.current
    val title = txn.payeeName ?: txn.vpa

    Column(
        Modifier
            .fillMaxWidth()
            .background(OffipeColors.Black)
            .pointerInput(txn.id) {
                detectTapGestures(
                    onLongPress = {
                        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                        onLongPress()
                    },
                    onTap = {
                        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                        expanded = !expanded
                    }
                )
            }
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Completed outgoing chip — pastel green, arrow out
            Box(
                Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(OffipeColors.Success),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.NorthEast,
                    contentDescription = "Sent",
                    tint = OffipeColors.Black,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(Modifier.width(13.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = OffipeType.TitleMedium,
                    color = OffipeColors.TextPrimary,
                    maxLines = 1
                )
                Spacer(Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LedDot(size = 5.dp, color = OffipeColors.Success)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = formatTimestamp(txn.timestamp),
                        style = OffipeType.BodySmall,
                        color = OffipeColors.TextMuted
                    )
                    if (txn.payeeName != null) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = txn.vpa,
                            style = OffipeType.BodySmall,
                            color = OffipeColors.TextMuted,
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(Modifier.width(10.dp))
            Text(
                text = "₹${txn.amount}",
                style = OffipeType.AmountMd,
                color = OffipeColors.TextPrimary
            )
        }

        if (!txn.note.isNullOrBlank()) {
            Spacer(Modifier.height(7.dp))
            Text(
                text = txn.note,
                style = OffipeType.BodySmall,
                color = OffipeColors.TextSecondary,
                modifier = Modifier.padding(start = 51.dp)
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column {
                Spacer(Modifier.height(12.dp))
                Hairline()
                Spacer(Modifier.height(11.dp))
                Tag("Carrier reply", color = OffipeColors.TextMuted)
                Spacer(Modifier.height(6.dp))
                Text(
                    text = txn.carrierReply,
                    style = OffipeType.BodyMedium,
                    color = OffipeColors.TextSecondary
                )
                Spacer(Modifier.height(14.dp))
                // "Pay again" — routes back to Pay with VPA + amount + note
                // pre-filled; the form still validates + requests PIN.
                GhostActionBar(
                    text = "Pay again",
                    onClick = onPayAgain,
                    modifier = Modifier.fillMaxWidth(),
                    height = 48.dp
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    TextKey(text = "Delete record", onClick = onDelete, color = OffipeColors.Danger)
                }
            }
        }
    }
}

private fun formatTimestamp(ts: Long): String {
    val now = System.currentTimeMillis()
    val delta = now - ts
    if (delta < 60_000) return "Just now"
    if (delta < 3_600_000) return "${delta / 60_000} min ago"
    if (delta < 86_400_000) return "${delta / 3_600_000} hours ago"
    if (delta < 172_800_000) return "Yesterday"
    val sdf = SimpleDateFormat("d MMM", Locale.getDefault())
    return sdf.format(Date(ts))
}
