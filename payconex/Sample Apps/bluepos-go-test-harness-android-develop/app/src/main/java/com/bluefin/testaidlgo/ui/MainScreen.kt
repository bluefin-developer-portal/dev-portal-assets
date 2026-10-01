package com.bluefin.testaidlgo.ui

import android.app.Activity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastFilter
import com.bluefin.blueposgo.sdk.response.PaymentResponse
import com.bluefin.testaidlgo.R
import com.bluefin.testaidlgo.data.AUTHORIZED
import com.bluefin.testaidlgo.data.REFUND
import com.bluefin.testaidlgo.data.prepareAmountNumber
import com.bluefin.testaidlgo.ui.events.MainScreenEvents
import com.bluefin.testaidlgo.ui.theme.ButtonGroup
import com.bluefin.testaidlgo.ui.theme.Purple80
import com.bluefin.testaidlgo.ui.theme.TestAIDLTheme
import com.bluefin.testaidlgo.ui.theme.actionButtonColors

@Composable
fun MainScreen(
    statusText: String,
    transList: List<PaymentResponse?>,
    refundsList: List<PaymentResponse>,
    checked: Boolean,
    onAction: (MainScreenEvents) -> Unit
) {
    var amountText by remember { mutableStateOf("1.00") }
    var tipText by remember { mutableStateOf("0.00") }
    var transactionId by remember { mutableStateOf("000000037074") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        stickyHeader {
            val context = LocalContext.current

            Column {
                Text(
                    modifier = Modifier.padding(8.dp),
                    text = stringResource(R.string.version, com.bluefin.blueposgo.sdk.BuildConfig.SDK_VERSION)
                )
                Row(
                    modifier = Modifier
                        .background(Color.DarkGray)
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        modifier = Modifier
                            .padding(start = 16.dp, end = 8.dp)
                            .height(40.dp)
                            .weight(1f),
                        colors = ButtonColors(
                            containerColor = Color.Red,
                            contentColor = Color.White,
                            disabledContentColor = Color.White,
                            disabledContainerColor = Color.Gray
                        ),
                        onClick = {
                            (context as Activity).finishAffinity()
                        }
                    ) {
                        Text(stringResource(R.string.exit))
                    }

                    Text(
                        text = stringResource(R.string.debug),
                    )
                    Switch(
                        modifier = Modifier.padding(start = 5.dp, end = 16.dp),
                        checked = checked,
                        onCheckedChange = { onAction(MainScreenEvents.DebugChange(!checked)) }
                    )
                }

                StatusText(
                    name = statusText,
                    modifier = Modifier
                        .background(Color.DarkGray)
                        .padding(top = 10.dp, start = 16.dp, end = 16.dp, bottom = 10.dp)
                        .fillMaxWidth()
                ) { onAction(MainScreenEvents.ClearStatusText) }
            }
        }

        item {
            Row(modifier = Modifier
                .padding(top = 10.dp, start = 16.dp, end = 16.dp)
                .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PriceEnterField(
                    labelText = stringResource(R.string.amount),
                    amountText = amountText,
                    modifier = Modifier.weight(1f)
                ) { newAmount -> amountText = newAmount }

                PriceEnterField(
                    labelText = stringResource(R.string.tip),
                    amountText = tipText,
                    modifier = Modifier.weight(1f),
                ) { newAmount -> tipText = newAmount }

            }

            OutlinedTextField(
                value = transactionId,
                onValueChange = { input -> transactionId = input },
                label = { Text(stringResource(R.string.transaction_id)) },
                singleLine = true,
                isError = transactionId.isBlank(),
                modifier = Modifier
                    .padding(top = 10.dp, start = 16.dp, end = 16.dp)
                    .fillMaxWidth()
            )
        }

        item {
            Row(
                modifier = Modifier
                    .padding(start = 16.dp, end = 16.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp),
                    colors = actionButtonColors(ButtonGroup.Service),
                    onClick = {
                        onAction(MainScreenEvents.InitClick)
                    }
                ) {
                    Text(stringResource(R.string.init))
                }

                Button(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp),
                    colors = actionButtonColors(ButtonGroup.Service),
                    onClick = {
                        onAction(MainScreenEvents.RebootClick)
                    }
                ) {
                    Text(stringResource(R.string.reboot))
                }
            }
        }

        item {
            Row(
                modifier = Modifier
                    .padding(start = 16.dp, end = 16.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp),
                    colors = actionButtonColors(ButtonGroup.Service),
                    onClick = {
                        onAction(MainScreenEvents.ConnectClick)
                    }
                ) {
                    Text(stringResource(R.string.connect))
                }

                Button(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp),
                    colors = actionButtonColors(ButtonGroup.Service),
                    onClick = {
                        onAction(MainScreenEvents.DisconnectClick)
                    }
                ) {
                    Text(stringResource(R.string.disconnect))
                }
            }
        }

        item {
            Row(
                modifier = Modifier
                    .padding(start = 16.dp, end = 16.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp),
                    colors = actionButtonColors(ButtonGroup.Service),
                    onClick = {
                        onAction(MainScreenEvents.ForgetClick)
                    }
                ) {
                    Text(stringResource(R.string.forget))
                }

                Spacer(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp),
                )
            }
        }

        item {
            Row(
                modifier = Modifier
                    .padding(start = 16.dp, end = 16.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp),
                    colors = actionButtonColors(ButtonGroup.Payment),
                    onClick = {
                        onAction(MainScreenEvents.ProcessClick(
                            prepareAmountNumber(amountText),
                            prepareAmountNumber(tipText),
                        ))
                    }
                ) {
                    Text(stringResource(R.string.payment))
                }

                Button(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp),
                    enabled = transactionId.isNotBlank(),
                    colors = actionButtonColors(ButtonGroup.Payment),
                    onClick = { onAction(
                        MainScreenEvents.RefundClick(
                            transactionId = transactionId,
                            amount = prepareAmountNumber(amountText)
                        )
                    ) }
                ) {
                    Text(stringResource(R.string.refund))
                }
            }
        }

        item {
            Row(
                modifier = Modifier
                    .padding(start = 16.dp, end = 16.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp),
                    colors = actionButtonColors(ButtonGroup.Payment),
                    onClick = {
                        onAction(MainScreenEvents.AuthClick(
                            prepareAmountNumber(amountText),
                            prepareAmountNumber(tipText)
                        ))
                    }
                ) {
                    Text(stringResource(R.string.auth))
                }


                Button(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp),
                    enabled = transactionId.isNotBlank(),
                    colors = actionButtonColors(ButtonGroup.Payment),
                    onClick = {
                        onAction(MainScreenEvents.CaptureClick(
                            transactionId = transactionId,
                            amount = prepareAmountNumber(amountText),
                        ))
                    }
                ) {
                    Text(stringResource(R.string.capture))
                }
            }
        }

        item {
            Row(
                modifier = Modifier
                    .padding(start = 16.dp, end = 16.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp),
                    colors = actionButtonColors(ButtonGroup.Utility),
                    onClick = { onAction(MainScreenEvents.SaveCardClick) }
                ) {
                    Text(stringResource(R.string.save_card))
                }

                Spacer(Modifier.weight(1f))
            }
        }

        item {
            Row(
                modifier = Modifier
                    .padding(start = 16.dp, end = 16.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp),
                    colors = actionButtonColors(ButtonGroup.Utility),
                    onClick = { onAction(MainScreenEvents.ClearDataClick(prepareAmountNumber(amountText))) }
                ) {
                    Text(stringResource(R.string.clear_data_read))
                }

                Spacer(Modifier.weight(1f))
            }
        }

        item {
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 5.dp, start = 16.dp, end = 16.dp)
                    .height(40.dp),
                colors = actionButtonColors(ButtonGroup.Transactions),
                onClick = { onAction(MainScreenEvents.GetTrListClick) }
            ) {
                Text(stringResource(R.string.transactions))
            }

            Spacer(Modifier.height(10.dp))
        }

        items(transList) { item ->
            val refunded: Double = refundsList.fastFilter {
                it.transactionDetails?.refundObject?.refundIds?.contains(
                    item?.transactionDetails?.transactionId
                ) ?: false }.sumOf { it.amount }

            val amountToRefund = try {
                (item?.transactionDetails?.amounts?.approved ?: "0.0").toDouble() - refunded
            } catch (_: Exception) {
                0.0
            }

            TransactionItem(
                item = item,
                amountToRefund = amountToRefund,
                onRefundClick = { id ->
                    onAction(MainScreenEvents.FullRefundClick(id))
                },
                onItemClick = { id -> transactionId = id }
            )
        }
    }
}

@Composable
fun TransactionItem(
    item: PaymentResponse?,
    amountToRefund: Double,
    onRefundClick: (String) -> Unit,
    onItemClick: (String) -> Unit
) {
    val amount = item?.transactionDetails?.amounts?.approved ?: 0
    val tip = item?.tip ?: 0
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .background(
                color = Color.Gray,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(4.dp)
            .clickable(enabled = true) {
                onItemClick(
                    item?.transactionDetails?.transactionId ?: ""
                )
            }

    ) {
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            text = buildAnnotatedString {
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = Purple80)) {
                    append(stringResource(R.string.status))
                }
                append(" ")
                append(item?.transactionDetails?.status?.toString() ?: "")
                append(" ")
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = Purple80)) {
                    append(stringResource(R.string.trans_type))
                }
                append(" ")
                append(item?.transactionDetails?.transactionType?.toString() ?: "")

            }
        )
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            text = buildAnnotatedString {
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = Purple80)) {
                            append(stringResource(R.string.amount_text))
                        }
                        append("$amount")
                        if (tip != 0.0) append(stringResource(R.string.tip_text, tip))
                        append(stringResource(R.string.to_refund,amountToRefund))
                    }
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = buildAnnotatedString {
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = Purple80)) {
                        append(stringResource(R.string.trans_id))
                    }
                    append(item?.transactionDetails?.transactionId)
                }
            )
            if (item?.type != REFUND && item?.type != AUTHORIZED)
                Button(
                    modifier = Modifier
                        .height(30.dp)
                        .padding(start = 10.dp),
                    enabled = amountToRefund > 0,
                    border = BorderStroke(if (amountToRefund > 0) 0.dp else 2.dp, Color.White),
                    onClick = {
                        item?.transactionDetails?.transactionId?.let { id -> onRefundClick(id) }
                    }
                ) {
                    Text(
                        color = if (amountToRefund == 0.0) Color.White else Color.Black,
                        fontSize = 10.sp,
                        text =
                            if (amountToRefund == 0.0) stringResource(R.string.already_refunded)
                            else stringResource(R.string.refund)
                    )
                }
        }
    }
}

@Composable
fun StatusText(
    name: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
    ) {
        Text(
            text = stringResource(R.string.status_is, name),
            modifier = Modifier
                .weight(1f)
                .heightIn(max = 200.dp)
                .verticalScroll(rememberScrollState())
        )
        Image(
            modifier = Modifier
                .padding(horizontal = 5.dp)
                .clickable(enabled = true) { onClick() },
            painter = painterResource(R.drawable.close),
            contentDescription = stringResource(R.string.close)
        )
    }
}

@Composable
fun PriceEnterField(
    labelText: String,
    amountText: String,
    modifier: Modifier,
    onAmountChange: (String) -> Unit
) {
    OutlinedTextField(
        value = amountText,
        onValueChange = { input ->
            val filtered = buildString {
                var dotSeen = false
                var decimals = 0

                for (ch in input) {
                    when {
                        ch.isDigit() && !dotSeen -> append(ch)
                        ch == '.' && !dotSeen -> {
                            append(ch)
                            dotSeen = true
                        }

                        ch.isDigit() && dotSeen && decimals < 2 -> {
                            append(ch)
                            decimals++
                        }
                    }
                }
            }

            onAmountChange(filtered)
        },
        label = { Text(labelText) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier
    )
}

@Preview(showBackground = true, name = "Light mode")
@Composable
fun MainLightPreview() {
    TestAIDLTheme(darkTheme = false, dynamicColor = false) {
        MainPreviewContent()
    }
}

@Preview(showBackground = true, name = "Dark mode", backgroundColor = 0xFF121212)
@Composable
fun MainDarkPreview() {
    TestAIDLTheme(darkTheme = true, dynamicColor = false) {
        MainPreviewContent()
    }
}

@Suppress("unused,UnusedMaterial3ScaffoldPaddingParameter")
@Composable
private fun MainPreviewContent() {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .imePadding()
                .navigationBarsPadding()
        ) { innerPadding ->
            MainScreen(
                statusText = "Test",
                transList = listOf(),
                refundsList = listOf(),
                checked = false
            ) {}
        }
}
