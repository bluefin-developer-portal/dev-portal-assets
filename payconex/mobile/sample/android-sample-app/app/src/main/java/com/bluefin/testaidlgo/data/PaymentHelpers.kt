package com.bluefin.testaidlgo.data

import android.util.Base64
import com.bluefin.blueposgo.sdk.request.ClearDataRequest
import com.bluefin.blueposgo.sdk.request.FullRefundRequest
import com.bluefin.blueposgo.sdk.request.InitRequest
import com.bluefin.blueposgo.sdk.request.PaymentRequest
import com.bluefin.blueposgo.sdk.request.PostProcessRequest
import com.bluefin.blueposgo.sdk.request.PostProcessType
import com.bluefin.testaidlgo.BuildConfig


/**
 * Adapter between sample configuration/UI cents and the bundled Android SDK 1.1.0 requests.
 * Keep SDK-specific field names and conversions here so a different UI can reuse the adapter.
 * The UI currently validates inputs before dispatch; these factories do not enforce amount
 * limits, transaction ownership or refund/capture eligibility for callers outside that UI.
 */
private data class PaymentConfig(
    val accountId: String,
    val apiKey: String,
    val apiSecret: String,
    val environment: String
)

// Values are compiled by app/build.gradle.kts from the ignored local file or build inputs.
// Rebuild after editing configuration. This presence check never prints credential values;
// MainActivity also checks the environment, while the vendor validates authentication.
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

/**
 * The SDK expects a full Basic header value: UTF-8 API key ID + ':' + API secret, then Base64.
 * NO_WRAP prevents embedded newlines. These are API credentials, not website login details.
 * Base64 is reversible encoding, not encryption; treat the resulting token as a credential.
 */
private fun buildBasicToken(apiKey: String, apiSecret: String): String {
    val credentials = "$apiKey:$apiSecret"
    val encoded = Base64.encodeToString(
        credentials.toByteArray(Charsets.UTF_8),
        Base64.NO_WRAP
    )
    return "Basic $encoded"
}

/**
 * [amount] and [tip] are integer cents (1234 -> 12.34). Convert only at this SDK boundary.
 * Android's SDK requires Double major units, unlike the iOS Decimal request. Integer parsing
 * avoids input-rounding errors but Double cannot preserve every possible Long cent value:
 * impose appropriate supported currency/processor limits when building a merchant app.
 * The caller changes type to "auth" or "save" for those flows; a save uses zero amount/tip.
 */
fun createPaymentRequest(amount: Long, tip: Long): PaymentRequest {
    val config = requirePaymentConfig()

    return PaymentRequest(
        amount = amount.toDouble() / 100,
        tip = tip.toDouble() / 100,
        type = "sale",
        notes = "External application", // Example metadata; replace with your own non-sensitive note.
        accountId = config.accountId,
        basicToken = buildBasicToken(config.apiKey, config.apiSecret),
        environment = config.environment
    )
}

/**
 * Partial refund of an existing vendor transaction ID, for [amount] cents. The SDK's
 * PostProcessRequest carries no credentials; initialize BluePOS Go before this operation.
 * The amount is a requested refund, not a locally calculated remaining refundable balance.
 */
fun createRefundRequest(transactionId: String, amount: Long): PostProcessRequest {
    return PostProcessRequest(
        amount = amount.toDouble() / 100,
        transactionId = transactionId,
        type = PostProcessType.REFUND
    )
}

/** Capture against an existing authorization ID for [amount] cents; eligibility is remote. */
fun createCaptureRequest(transactionId: String, amount: Long): PostProcessRequest {
    return PostProcessRequest(
        amount = amount.toDouble() / 100,
        transactionId = transactionId,
        type = PostProcessType.CAPTURE
    )
}

/**
 * Reader clear-data demonstration using [amount] cents and the SDK's timeOut field.
 * The callback intentionally consumes status only; do not route card payloads into UI/logs.
 */
fun createClearDataRequest(amount: Long): ClearDataRequest {
    return ClearDataRequest(
        amount = amount.toDouble() / 100,
        timeOut = 60
    )
}

/** Full refund uses only the referenced ID; it deliberately ignores the UI amount/tip. */
fun createFullRefundRequest(id: String): FullRefundRequest {
    return FullRefundRequest(
        transactionId = id
    )
}


/** Establish BluePOS Go's payment configuration before payment, post-processing or reader calls. */
fun createInitRequest(): InitRequest {
    val config = requirePaymentConfig()

    return InitRequest(
        accountId = config.accountId,
        basicToken = buildBasicToken(config.apiKey, config.apiSecret),
        environment = config.environment
    )
}
