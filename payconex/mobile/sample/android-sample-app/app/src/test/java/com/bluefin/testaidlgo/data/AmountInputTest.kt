package com.bluefin.testaidlgo.data

import org.junit.Assert.*
import org.junit.Test

class AmountInputTest {
    @Test fun acceptsExactDecimalAmounts() {
        mapOf("1.00" to 100L, " 12,34 " to 1234L, "0" to 0L,
            "001.2" to 120L, "92233720368547758.07" to Long.MAX_VALUE)
            .forEach { (input, expected) -> assertEquals(input, expected, parseAmountCents(input)) }
    }
    @Test fun rejectsAmbiguousOrLossyAmounts() {
        listOf("", " ", "-1", "+1", "1.001", "1,234.56", "1e2", "NaN", "Infinity", ".50", "1.",
            "92233720368547758.08", "999999999999999999999999999999999999999999999999.00")
            .forEach { assertNull(it, parseAmountCents(it)) }
    }
}
