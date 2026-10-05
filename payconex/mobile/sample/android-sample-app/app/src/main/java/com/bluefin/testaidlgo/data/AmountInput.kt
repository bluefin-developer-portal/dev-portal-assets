package com.bluefin.testaidlgo.data

/**
 * Parses this sample's two-decimal currency entry into exact integer cents ("12.34" -> 1234).
 * A single decimal comma is accepted like a dot; grouping separators, signs, exponents,
 * blanks and more than two decimal places are rejected rather than rounded. Overflow is null.
 * Zero is valid here: MainScreen requires a positive payment amount but permits a zero tip.
 *
 * This is pure Kotlin so host-side AmountInputTest needs neither Android nor BluePOS Go.
 * It is not a general currency/locale parser and does not apply merchant/processor limits;
 * changing currency requires reviewing the scale, UI labels and SDK request conversion together.
 */
fun parseAmountCents(text: String): Long? {
    val normalized = text.trim().replace(',', '.')
    if (!Regex("[0-9]+(?:\\.[0-9]{1,2})?").matches(normalized)) return null
    return try {
        normalized.toBigDecimal().movePointRight(2).longValueExact()
    } catch (_: ArithmeticException) {
        null
    } catch (_: NumberFormatException) {
        null
    }
}
