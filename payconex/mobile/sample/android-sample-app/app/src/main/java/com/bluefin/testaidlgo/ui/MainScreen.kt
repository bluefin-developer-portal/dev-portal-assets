package com.bluefin.testaidlgo.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.bluefin.testaidlgo.R
import com.bluefin.testaidlgo.ui.theme.BluefinColors
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.bluefin.blueposgo.sdk.response.PaymentResponse
import com.bluefin.testaidlgo.BuildConfig
import com.bluefin.testaidlgo.data.parseAmountCents
import com.bluefin.testaidlgo.ui.events.MainScreenEvents
import java.util.Locale


/**
 * Shared six-section developer layout. This screen renders state and emits explicit intents;
 * SDK calls, configuration and callbacks stay in MainActivity/PaymentHelpers.
 * Keep operation controls aligned with CheckoutView.swift when extending both samples, while
 * retaining the documented platform capability differences rather than inventing SDK methods.
 */
@Composable
fun MainScreen(
    statusText: String,
    transList: List<PaymentResponse>,
    transactionId: String,
    onTransactionIdChange: (String) -> Unit,
    isConfigured: Boolean,
    checked: Boolean,
    darkMode: Boolean,
    diagnosticsEnabled: Boolean,
    diagnostics: List<String>,
    connectionStatus: String,
    onAction: (MainScreenEvents) -> Unit
) {
    // Saveable text preserves unfinished edits during normal Activity recreation. It is not a
    // pending-payment record. Keep text separate from parsed cents so invalid input stays editable.
    var amountText by rememberSaveable { mutableStateOf("1.00") }
    var tipText by rememberSaveable { mutableStateOf("0.00") }
    // Decimal keyboard selection is only a hint. Parse pasted/typed text, default a blank tip
    // to zero, and guard actions independently: save/full refund do not need the amount field.
    // The !! uses below rely on their button-enabled checks; non-UI callers need their own validation.
    val amount = parseAmountCents(amountText)
    val tip = parseAmountCents(tipText.ifBlank { "0" })
    val validAmount = amount != null && amount > 0
    val hasId = transactionId.isNotBlank()

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { BluefinHeader(darkMode) { onAction(MainScreenEvents.DarkModeChange(it)) } }
        item {
            Panel("Status") {
                Text(statusText.ifBlank { "No result yet." })
                ActionButton("Initialize", enabled = isConfigured, primary = true) { onAction(MainScreenEvents.InitClick) }
            }
        }
        item {
            Panel("Payment details") {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MoneyField("Amount (USD)", amountText, !validAmount, Modifier.weight(1f)) { amountText = it }
                    MoneyField("Tip (USD)", tipText, tip == null, Modifier.weight(1f)) { tipText = it }
                }
                if (!validAmount || tip == null) {
                    Text("Enter an amount greater than zero and a non-negative tip, with at most two decimal places.",
                        color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                OutlinedTextField(value = transactionId, onValueChange = onTransactionIdChange,
                    label = { Text("Transaction ID") }, placeholder = { Text("Enter or select a transaction") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(4.dp))
                Hint("Capture and refunds use the transaction ID. Partial refund and capture use Amount.")
            }
        }
        item {
            Panel("Payments") {
                ActionRow {
                    ActionButton("Sale", Modifier.weight(1f), isConfigured && validAmount && tip != null, primary = true) {
                        onAction(MainScreenEvents.ProcessClick(amount!!, tip!!))
                    }
                    ActionButton("Authorization", Modifier.weight(1f), isConfigured && validAmount && tip != null) {
                        onAction(MainScreenEvents.AuthClick(amount!!, tip!!))
                    }
                }
                ActionRow {
                    ActionButton("Capture", Modifier.weight(1f), isConfigured && validAmount && hasId) {
                        onAction(MainScreenEvents.CaptureClick(transactionId.trim(), amount!!))
                    }
                    ActionButton("Save card", Modifier.weight(1f), isConfigured) { onAction(MainScreenEvents.SaveCardClick) }
                }
                ActionRow {
                    ActionButton("Full refund", Modifier.weight(1f), isConfigured && hasId) {
                        onAction(MainScreenEvents.FullRefundClick(transactionId.trim()))
                    }
                    ActionButton("Partial refund", Modifier.weight(1f), isConfigured && validAmount && hasId) {
                        onAction(MainScreenEvents.RefundClick(transactionId.trim(), amount!!))
                    }
                }
            }
        }
        item {
            Panel("Reader") {
                ActionRow {
                    ActionButton("Reboot reader", Modifier.weight(1f), isConfigured) { onAction(MainScreenEvents.RebootClick) }
                    ActionButton("Clear-data read", Modifier.weight(1f), isConfigured && validAmount) {
                        onAction(MainScreenEvents.ClearDataClick(amount!!))
                    }
                }
                ActionRow {
                    ActionButton("Connect", Modifier.weight(1f), isConfigured) { onAction(MainScreenEvents.ConnectClick) }
                    ActionButton("Disconnect", Modifier.weight(1f), isConfigured) { onAction(MainScreenEvents.DisconnectClick) }
                }
                ActionRow {
                    ActionButton("Forget reader", Modifier.weight(1f), isConfigured) { onAction(MainScreenEvents.ForgetClick) }
                    Spacer(Modifier.weight(1f))
                }
                Hint("Reader controls require BluePOS Go and a physical reader. Clear card data is not displayed or logged.")
            }
        }
        item {
            Panel("Transactions") {
                ActionButton("Refresh transactions", enabled = isConfigured) { onAction(MainScreenEvents.GetTrListClick) }
                Hint("Select a transaction to fill its ID for capture or refund.")
                if (transList.isEmpty()) Hint("No transactions loaded.")
            }
        }
        // Selecting a row copies only its vendor ID. Do not infer refund eligibility or issue
        // a financial request from selection; the explicit Payments buttons perform dispatch.
        itemsIndexed(transList) { _, transaction ->
            val id = transaction.transactionDetails?.transactionId.orEmpty()
            OutlinedCard(onClick = { onTransactionIdChange(id) }, enabled = id.isNotBlank(),
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(if (id.isNotBlank() && id == transactionId) 2.dp else 1.dp,
                    if (id.isNotBlank() && id == transactionId) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("${transaction.type.orEmpty().replaceFirstChar { it.uppercase() }} · ${transaction.transactionDetails?.status ?: "Unknown"}", fontWeight = FontWeight.SemiBold)
                    Text("ID: ${id.ifBlank { "Unavailable" }}")
                    val approved = transaction.transactionDetails?.amounts?.approved ?: String.format(Locale.US, "%.2f", transaction.amount)
                    Hint("Amount: $approved USD · Tip: ${String.format(Locale.US, "%.2f", transaction.tip)} USD")
                    if (id.isNotBlank() && id == transactionId) Text("Selected", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
        item {
            Panel("Developer tools") {
                // Diagnostics are local metadata, while SDK debug mode changes the remote service.
                // This SDK has no cancel-pending method; retain the explanatory disabled control.
                Hint("Transport: Android AIDL service + activity handoff")
                Hint(connectionStatus)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Show diagnostics", modifier = Modifier.weight(1f))
                    Switch(checked = diagnosticsEnabled, onCheckedChange = { onAction(MainScreenEvents.DiagnosticsChange(it)) })
                }
                ActionRow {
                    ActionButton("Clear result", Modifier.weight(1f)) { onAction(MainScreenEvents.ClearStatusText) }
                    ActionButton("Cancel pending", Modifier.weight(1f), enabled = false) {}
                }
                Hint("Cancel an active operation in BluePOS Go. This Android SDK cannot clear pending operations from the sample.")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("SDK debug mode (Android)", modifier = Modifier.weight(1f))
                    Switch(checked = checked, enabled = isConfigured, onCheckedChange = { onAction(MainScreenEvents.DebugChange(it)) })
                }
                Hint("SDK ${com.bluefin.blueposgo.sdk.BuildConfig.SDK_VERSION}")
                if (diagnosticsEnabled) {
                    Hint("Local request/callback trace; no credentials or card payloads. Latest 50 events.")
                    ActionButton("Clear diagnostics") { onAction(MainScreenEvents.ClearDiagnostics) }
                    if (diagnostics.isEmpty()) Hint("No diagnostic events yet.")
                    Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp)).padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        diagnostics.forEach { Text(it, style = MaterialTheme.typography.bodySmall, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace) }
                    }
                }
            }
        }
    }
}

@Composable
private fun Panel(title: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.width(3.dp).height(18.dp).background(BluefinColors.Sky))
                Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
            }
            content()
        }
    }
}

@Composable
private fun ActionRow(content: @Composable RowScope.() -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), content = content)
}

// Presentation-only components: keep SDK/business rules at the screen/adapter boundaries.
// All operation buttons get the same enabled styling and minimum touch height here.
@Composable
private fun ActionButton(label: String, modifier: Modifier = Modifier, enabled: Boolean = true,
                         primary: Boolean = false, onClick: () -> Unit) {
    val colors = if (primary) ButtonDefaults.buttonColors(containerColor = BluefinColors.Yellow, contentColor = BluefinColors.Navy)
        else ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
    OutlinedButton(onClick = onClick, enabled = enabled,
        modifier = modifier.fillMaxWidth().heightIn(min = 48.dp),
        shape = RoundedCornerShape(4.dp), colors = colors,
        border = if (primary && enabled) null else BorderStroke(1.dp, if (enabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.55f) else MaterialTheme.colorScheme.outlineVariant),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun MoneyField(label: String, value: String, invalid: Boolean, modifier: Modifier, onChange: (String) -> Unit) {
    OutlinedTextField(value = value, onValueChange = onChange, label = { Text(label) },
        modifier = modifier, singleLine = true, isError = invalid, shape = RoundedCornerShape(4.dp),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
}

@Composable
private fun Hint(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun BluefinHeader(darkMode: Boolean, onDarkModeChange: (Boolean) -> Unit) {
    val wash = MaterialTheme.colorScheme.surfaceVariant
    Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxWidth().drawBehind {
            drawPath(Path().apply {
                moveTo(size.width * 0.64f, 0f)
                lineTo(size.width, 0f)
                lineTo(size.width, size.height)
                lineTo(size.width * 0.22f, size.height)
                close()
            }, wash)
        }) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Select explicitly from the app preference; drawable-night selection alone
                    // would follow the device and could disagree with the in-app appearance toggle.
                    Image(painterResource(if (darkMode) R.drawable.bluefin_logo_dark else R.drawable.bluefin_logo_light), contentDescription = "Bluefin",
                        contentScale = ContentScale.Fit, modifier = Modifier.width(144.dp).height(27.dp))
                    Spacer(Modifier.weight(1f))
                    Text(BuildConfig.BLUEPOS_ENVIRONMENT.trim().uppercase(Locale.ROOT),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(4.dp)).padding(8.dp))
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("BluePOS Go Sample", style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.semantics { heading() })
                        Text("SDK developer sample", color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Dark mode", style = MaterialTheme.typography.labelSmall)
                        Switch(checked = darkMode, onCheckedChange = onDarkModeChange,
                            modifier = Modifier.semantics { contentDescription = "Dark mode" })
                    }
                }
            }
            Box(Modifier.fillMaxWidth().height(3.dp).background(BluefinColors.Yellow))
        }
    }
}
