import Foundation

/// Pure input rules matching Android AmountInput.kt: two-decimal, non-negative currency entry.
/// Keep integer cents/Decimal through validation; do not introduce Double conversion here.
/// This sample assumes USD scale. It does not implement generic locales, multi-currency scales
/// or merchant/processor amount limits; review all three before reusing it for another currency.
enum PaymentInput {
    /// "12.34" and "12,34" become 1234 cents. Reject blank, signed, grouped, exponential or
    /// over-precise values and Int64 overflow. Zero is allowed at this parsing layer; callers
    /// decide whether their operation requires a positive value. Invalid input returns nil.
    static func cents(_ text: String) -> Int64? {
        let normalized = text.trimmingCharacters(in: .whitespacesAndNewlines).replacingOccurrences(of: ",", with: ".")
        guard normalized.range(of: "^[0-9]+(?:\\.[0-9]{1,2})?$", options: .regularExpression) != nil,
              let decimal = Decimal(string: normalized, locale: Locale(identifier: "en_US_POSIX")) else { return nil }
        let cents = decimal * 100
        guard cents >= 0, cents <= Decimal(Int64.max) else { return nil }
        return NSDecimalNumber(decimal: cents).int64Value
    }

    /// Positive major-unit Decimal for payment, capture, partial refund and clear-data requests.
    static func amount(_ text: String) -> Decimal? {
        guard let value = cents(text), value > 0 else { return nil }
        return Decimal(value) / 100
    }

    /// A blank tip intentionally means zero; negative/invalid text must never silently become zero.
    static func tip(_ text: String) -> Decimal? {
        let input = text.trimmingCharacters(in: .whitespacesAndNewlines)
        guard let value = cents(input.isEmpty ? "0" : input) else { return nil }
        return Decimal(value) / 100
    }

    /// SDK capture expects an ungrouped dot-decimal string regardless of device locale.
    /// Call with an already validated, two-decimal value. This formatter is not an input validator;
    /// its precision/fallback must not be used to repair invalid or arbitrary monetary input.
    static func formatted(_ amount: Decimal) -> String {
        let formatter = NumberFormatter()
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.numberStyle = .decimal
        formatter.usesGroupingSeparator = false
        formatter.minimumFractionDigits = 2
        formatter.maximumFractionDigits = 2
        return formatter.string(from: NSDecimalNumber(decimal: amount)) ?? "0.00"
    }
}
