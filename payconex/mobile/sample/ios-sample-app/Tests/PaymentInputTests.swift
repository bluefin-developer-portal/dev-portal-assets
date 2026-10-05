import Foundation

@main struct PaymentInputTests {
    static func main() {
        for (input, expected) in [("1.00", Int64(100)), (" 12,34 ", 1234), ("0", 0), ("001.2", 120), ("92233720368547758.07", Int64.max)] {
            precondition(PaymentInput.cents(input) == expected, "Exact amount: \(input)")
        }
        for input in ["", " ", "-1", "+1", "1.001", "1,234.56", "1e2", "NaN", "Infinity", ".50", "1.",
                      "92233720368547758.08", "999999999999999999999999999999999999999999999999.00"] {
            precondition(PaymentInput.cents(input) == nil, "Reject malformed amount: \(input)")
        }
        precondition(PaymentInput.amount("0") == nil)
        precondition(PaymentInput.tip("") == 0)
        precondition(PaymentInput.tip("-1") == nil)
        precondition(PaymentInput.formatted(PaymentInput.amount("12,34")!) == "12.34")
        print("Payment input tests passed")
    }
}
