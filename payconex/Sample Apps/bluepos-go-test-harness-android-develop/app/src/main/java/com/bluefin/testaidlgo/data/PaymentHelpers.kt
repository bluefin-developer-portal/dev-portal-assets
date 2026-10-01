package com.bluefin.testaidlgo.data

import android.util.Base64
import com.bluefin.blueposgo.sdk.request.ClearDataRequest
import com.bluefin.blueposgo.sdk.request.FullRefundRequest
import com.bluefin.blueposgo.sdk.request.InitRequest
import com.bluefin.blueposgo.sdk.request.PaymentRequest
import com.bluefin.blueposgo.sdk.request.PostProcessRequest
import com.bluefin.blueposgo.sdk.request.PostProcessType
import com.bluefin.testaidlgo.BuildConfig
import java.math.RoundingMode

const val REFUND = "refund"
const val AUTHORIZED = "AUTHORIZED"

private data class PaymentConfig(
    val accountId: String,
    val apiKey: String,
    val apiSecret: String,
    val environment: String
)

private fun requirePaymentConfig(): PaymentConfig {
    val config = PaymentConfig(
        accountId = BuildConfig.BLUEPOS_ACCOUNT_ID,
        apiKey = BuildConfig.BLUEPOS_API_KEY,
        apiSecret = BuildConfig.BLUEPOS_API_SECRET,
        environment = BuildConfig.BLUEPOS_ENVIRONMENT
    )
    val missing = listOf(
        "BLUEPOS_ACCOUNT_ID" to config.accountId,
        "BLUEPOS_API_KEY" to config.apiKey,
        "BLUEPOS_API_SECRET" to config.apiSecret,
        "BLUEPOS_ENVIRONMENT" to config.environment
    ).filter { (_, value) -> value.isBlank() }
        .joinToString { (name, _) -> name }

    check(missing.isEmpty()) {
        "Missing payment configuration: $missing. Add values to payment.properties, Gradle properties, or environment variables."
    }

    return config
}

private fun buildBasicToken(apiKey: String, apiSecret: String): String {
    val credentials = "$apiKey:$apiSecret"
    val encoded = Base64.encodeToString(
        credentials.toByteArray(Charsets.UTF_8),
        Base64.NO_WRAP
    )
    return "Basic $encoded"
}

fun createPaymentRequest(amount: Long, tip: Long): PaymentRequest {
    val config = requirePaymentConfig()

    return PaymentRequest(
        amount = amount.toDouble() / 100,
        tip = tip.toDouble() / 100,
        type = "sale",
        notes = "External application", // not really needed
        accountId = config.accountId,
        basicToken = buildBasicToken(config.apiKey, config.apiSecret),
        environment = config.environment
    )
}

fun createRefundRequest(transactionId: String, amount: Long): PostProcessRequest {
    return PostProcessRequest(
        amount = amount.toDouble() / 100,
        transactionId = transactionId,
        type = PostProcessType.REFUND
    )
}

fun createCaptureRequest(transactionId: String, amount: Long): PostProcessRequest {
    return PostProcessRequest(
        amount = amount.toDouble() / 100,
        transactionId = transactionId,
        type = PostProcessType.CAPTURE
    )
}

fun createClearDataRequest(amount: Long): ClearDataRequest {
    return ClearDataRequest(
        amount = amount.toDouble() / 100,
        timeOut = 60
    )
}
fun createFullRefundRequest(id: String): FullRefundRequest {
    return FullRefundRequest(
        transactionId = id
    )
}


fun createInitRequest(): InitRequest {
    val config = requirePaymentConfig()

    return InitRequest(
        accountId = config.accountId,
        basicToken = buildBasicToken(config.apiKey, config.apiSecret),
        environment = config.environment
    )
}

fun prepareAmountNumber(amountText: String): Long = amountText
    .toBigDecimalOrNull()
    ?.setScale(2, RoundingMode.DOWN)
    ?.movePointRight(2)
    ?.toLong() ?: 0
