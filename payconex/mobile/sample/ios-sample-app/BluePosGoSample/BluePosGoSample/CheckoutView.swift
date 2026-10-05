import SwiftUI


/// Presentation for the shared six-section sample layout; all SDK work lives in CheckoutModel.
/// SceneDelegate injects long-lived model/appearance objects. Do not instantiate a model in body:
/// SwiftUI redraws would lose editable state and the completion target during app handoff.
/// When adding features, update Android MainScreen.kt and document genuine SDK differences.
struct CheckoutView: View {
    @EnvironmentObject private var model: CheckoutModel
    @EnvironmentObject private var appearance: SampleAppearance

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                BluefinHeader(environment: model.environment, isDarkMode: $appearance.isDarkMode)
                SamplePanel("Status") {
                    Text(model.statusText).font(.body).fixedSize(horizontal: false, vertical: true)
                    SampleButton("Initialize", enabled: model.canStart, primary: true) { model.initialize() }
                }
                SamplePanel("Payment details") {
                    HStack(alignment: .top, spacing: 12) {
                        SampleField("Amount (USD)", text: $model.amount, decimal: true)
                        SampleField("Tip (USD)", text: $model.tip, decimal: true)
                    }
                    if !model.hasValidAmount || !model.hasValidTip {
                        Text("Enter an amount greater than zero and a non-negative tip, with at most two decimal places.")
                            .font(.footnote).foregroundColor(.red)
                    }
                    SampleField("Transaction ID", text: $model.transactionId, placeholder: "Enter or select a transaction")
                    SampleHint("Capture and refunds use the transaction ID. Partial refund and capture use Amount.")
                }
                // Enabled checks give immediate feedback; CheckoutModel revalidates inputs on
                // dispatch. canStart is a local busy/configuration guard, not remote readiness.
                SamplePanel("Payments") {
                    HStack(spacing: 10) {
                        SampleButton("Sale", enabled: model.canStart && model.hasValidAmount && model.hasValidTip, primary: true) { model.takePayment(type: .sale) }
                        SampleButton("Authorization", enabled: model.canStart && model.hasValidAmount && model.hasValidTip) { model.takePayment(type: .auth) }
                    }
                    HStack(spacing: 10) {
                        SampleButton("Capture", enabled: model.canStart && model.hasValidAmount && model.hasTransactionId) { model.capture() }
                        SampleButton("Save card", enabled: model.canStart) { model.saveCard() }
                    }
                    HStack(spacing: 10) {
                        SampleButton("Full refund", enabled: model.canStart && model.hasTransactionId) { model.fullRefund() }
                        SampleButton("Partial refund", enabled: model.canStart && model.hasValidAmount && model.hasTransactionId) { model.partialRefund() }
                    }
                }
                SamplePanel("Reader") {
                    HStack(spacing: 10) {
                        SampleButton("Reboot reader", enabled: model.canStart) { model.rebootReader() }
                        SampleButton("Clear-data read", enabled: model.canStart && model.hasValidAmount) { model.clearDataRead() }
                    }
                    HStack(spacing: 10) {
                        SampleButton("Connect", enabled: false) {}
                        SampleButton("Disconnect", enabled: false) {}
                    }
                    HStack(spacing: 10) {
                        SampleButton("Forget reader", enabled: false) {}
                        Color.clear.frame(maxWidth: .infinity, maxHeight: 0)
                    }
                    // Layout parity does not imply API parity. These disabled controls explain
                    // that reader management belongs in BluePOS Go for this SDK version.
                    SampleHint("Connect, disconnect and forget the reader in BluePOS Go; the iOS SDK has no direct commands for these actions.")
                    SampleHint("Reader controls require BluePOS Go and a physical reader. Clear card data is not displayed or logged.")
                }
                SamplePanel("Transactions") {
                    SampleButton("Refresh transactions", enabled: model.canStart) { model.loadTransactions() }
                    SampleHint("Select a transaction to fill its ID for capture or refund.")
                    if model.transactions.isEmpty { SampleHint("No transactions loaded.") }
                }
                // Selection fills only the vendor ID; it must not send a refund/capture or infer
                // the remaining refundable amount. The list is a replaceable inspection snapshot.
                ForEach(Array(model.transactions.enumerated()), id: \.offset) { _, transaction in
                    let id = transaction.transactionDetails.transactionId ?? ""
                    let selected = !id.isEmpty && id == model.transactionId
                    Button(action: { model.transactionId = id }) {
                        VStack(alignment: .leading, spacing: 6) {
                            Text("\((transaction.type?.rawValue ?? "Unknown").capitalized) · \(transaction.transactionDetails.status?.rawValue ?? "Unknown")").font(.subheadline).bold()
                            Text("ID: \(id.isEmpty ? "Unavailable" : id)").font(.subheadline)
                            SampleHint("Amount: \(transaction.transactionDetails.approvedAmount ?? PaymentInput.formatted(transaction.amount)) USD · Tip: \(PaymentInput.formatted(transaction.tip ?? 0)) USD")
                            if selected { Text("Selected").font(.caption).foregroundColor(BluefinTheme.action) }
                        }
                        .frame(maxWidth: .infinity, alignment: .leading).padding(16)
                        .background(BluefinTheme.surface).cornerRadius(8)
                        .overlay(RoundedRectangle(cornerRadius: 8).stroke(selected ? BluefinTheme.action : BluefinTheme.border, lineWidth: selected ? 2 : 1))
                    }
                    .buttonStyle(PlainButtonStyle()).disabled(id.isEmpty)
                }
                SamplePanel("Developer tools") {
                    SampleHint("Transport: iOS URL scheme + callback")
                    SampleHint("Callback: mymerchantapp://bluepos/<operation>")
                    // This toggles the sample's metadata trace, not SDK debug mode. There is no
                    // supported iOS SDK debug flag, so do not add a decorative debug switch.
                    Toggle("Show diagnostics", isOn: $model.diagnosticsEnabled).bluefinToggleTint()
                    HStack(spacing: 10) {
                        SampleButton("Clear result") { model.clearResult() }
                        SampleButton("Cancel pending", enabled: model.isBusy) { model.cancelPending() }
                    }
                    SampleHint("Cancel pending clears local SDK callbacks only. It does not reverse a payment or stop the reader; check the outcome in BluePOS Go.")
                    SampleHint("SDK 1.0.0-dev.7.feb5e9a8")
                    if model.diagnosticsEnabled {
                        SampleHint("Local request/callback trace; no credentials or card payloads. Latest 50 events.")
                        SampleButton("Clear diagnostics") { model.diagnostics.removeAll() }
                        if model.diagnostics.isEmpty { SampleHint("No diagnostic events yet.") }
                        VStack(alignment: .leading, spacing: 6) {
                            ForEach(Array(model.diagnostics.enumerated()), id: \.offset) { _, line in
                                Text(line).font(.system(.footnote, design: .monospaced)).fixedSize(horizontal: false, vertical: true)
                            }
                        }
                        .frame(maxWidth: .infinity, alignment: .leading).padding(12)
                        .background(BluefinTheme.wash).cornerRadius(4)
                    }
                }
            }
            .padding(16)
        }
        .foregroundColor(BluefinTheme.ink)
        .accentColor(BluefinTheme.action)
        .background(BluefinTheme.background.edgesIgnoringSafeArea(.all))
    }
}

private struct SamplePanel<Content: View>: View {
    let title: String
    let content: Content
    init(_ title: String, @ViewBuilder content: () -> Content) {
        self.title = title
        self.content = content()
    }
    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(spacing: 8) {
                Rectangle().fill(BluefinTheme.sky).frame(width: 3, height: 18)
                Text(title).font(.headline).foregroundColor(BluefinTheme.ink).accessibility(addTraits: .isHeader)
            }
            content
        }
        .frame(maxWidth: .infinity, alignment: .leading).padding(16)
        .background(BluefinTheme.surface).cornerRadius(8)
        .overlay(RoundedRectangle(cornerRadius: 8).stroke(BluefinTheme.border.opacity(0.55), lineWidth: 1))
    }
}

// Shared presentation/keyboard behavior only. Keep validation and SDK dispatch out of this
// component so every action still goes through the model when the layout is customized.
private struct SampleButton: View {
    let title: String
    var enabled: Bool
    var primary: Bool
    let action: () -> Void
    init(_ title: String, enabled: Bool = true, primary: Bool = false, action: @escaping () -> Void) {
        self.title = title; self.enabled = enabled; self.primary = primary; self.action = action
    }
    var body: some View {
        Button(action: {
            UIApplication.shared.sendAction(#selector(UIResponder.resignFirstResponder), to: nil, from: nil, for: nil)
            action()
        }) {
            Text(title).font(.subheadline).fontWeight(.medium).multilineTextAlignment(.center)
                .frame(maxWidth: .infinity, minHeight: 48)
                .foregroundColor(enabled ? (primary ? BluefinTheme.navy : BluefinTheme.action) : BluefinTheme.muted.opacity(0.65))
                .background(primary && enabled ? BluefinTheme.yellow : Color.clear)
                .cornerRadius(4)
                .overlay(RoundedRectangle(cornerRadius: 4).stroke(primary && enabled ? Color.clear : (enabled ? BluefinTheme.action.opacity(0.55) : BluefinTheme.border.opacity(0.55)), lineWidth: 1))
        }
        .buttonStyle(PlainButtonStyle()).disabled(!enabled)
    }
}

private struct SampleField: View {
    let label: String
    @Binding var text: String
    var decimal = false
    var placeholder = ""
    init(_ label: String, text: Binding<String>, decimal: Bool = false, placeholder: String = "") {
        self.label = label; self._text = text; self.decimal = decimal; self.placeholder = placeholder
    }
    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(label).font(.caption).foregroundColor(BluefinTheme.muted)
            // A decimal keypad does not validate paste/input. PaymentInput performs validation.
            TextField(placeholder, text: $text).keyboardType(decimal ? .decimalPad : .default)
                .autocapitalization(.none).disableAutocorrection(true)
                .accessibility(label: Text(label))
                .padding(12).background(BluefinTheme.surface)
                .overlay(RoundedRectangle(cornerRadius: 4).stroke(BluefinTheme.border, lineWidth: 1))
        }
        .frame(maxWidth: .infinity)
    }
}

private struct SampleHint: View {
    let text: String
    init(_ text: String) { self.text = text }
    var body: some View { Text(text).font(.footnote).foregroundColor(BluefinTheme.muted).fixedSize(horizontal: false, vertical: true) }
}

private extension Font {
    static var title2Compat: Font {
        if #available(iOS 14, *) { return .title2 }
        return .title
    }
}

private struct BluefinHeader: View {
    let environment: String
    @Binding var isDarkMode: Bool
    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            VStack(alignment: .leading, spacing: 14) {
                HStack {
                    Image("BluefinLogo").resizable().scaledToFit()
                        .frame(width: 144, height: 27).accessibility(label: Text("Bluefin"))
                    Spacer(minLength: 12)
                    Text(environment).font(.caption).bold().foregroundColor(BluefinTheme.ink)
                        .padding(8).background(BluefinTheme.wash).cornerRadius(4)
                }
                HStack(alignment: .center, spacing: 12) {
                    VStack(alignment: .leading, spacing: 4) {
                        Text("BluePOS Go Sample").font(.title2Compat).bold().accessibility(addTraits: .isHeader)
                        Text("SDK developer sample").font(.subheadline).foregroundColor(BluefinTheme.muted)
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    VStack(spacing: 4) {
                        Text("Dark mode").font(.caption)
                        Toggle("Dark mode", isOn: $isDarkMode).labelsHidden().bluefinToggleTint().fixedSize()
                    }
                }
            }
            .padding(16)
            Rectangle().fill(BluefinTheme.yellow).frame(height: 3)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(BluefinAngle().fill(BluefinTheme.wash))
        .background(BluefinTheme.surface).cornerRadius(8)
    }
}
