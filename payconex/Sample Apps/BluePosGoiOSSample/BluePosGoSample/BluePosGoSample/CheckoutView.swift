import SwiftUI

struct CheckoutView: View {
    @EnvironmentObject private var model: CheckoutModel

    var body: some View {
        NavigationView {
            Form {
                Section(header: Text("Credentials")) {
                    TextField("Basic token", text: $model.basicToken)
                        .autocapitalization(.none)
                        .disableAutocorrection(true)
                    TextField("Account ID", text: $model.accountId)
                        .autocapitalization(.none)
                        .disableAutocorrection(true)
                    Picker("Environment", selection: $model.environment) {
                        Text("STAGING").tag("STAGING")
                        Text("CERT").tag("CERT")
                        Text("PROD").tag("PROD")
                    }
                }

                Section(header: Text("Amount")) {
                    TextField("Amount", text: $model.amount)
                        .keyboardType(.decimalPad)
                    TextField("Tip", text: $model.tip)
                        .keyboardType(.decimalPad)
                    TextField("Transaction ID", text: $model.transactionId)
                        .autocapitalization(.none)
                        .disableAutocorrection(true)
                }

                Section(header: Text("Demo return")) {
                    Picker("Outcome", selection: $model.demoOutcome) {
                        Text("Approved").tag("approved")
                        Text("Declined").tag("declined")
                        Text("Cancelled").tag("cancelled")
                        Text("Failed").tag("failed")
                        Text("Masked").tag("masked")
                    }
                    Text("The local SDK stand-in completes by reopening this callback URL. BluePOS Go does that itself when you use the real package.")
                        .font(.footnote)
                        .foregroundColor(.secondary)
                }

                Section(header: Text("Operations")) {
                    Button("Initialize") { model.initialize() }
                    Button("Sale") { model.takePayment(type: .sale) }
                    Button("Authorization") { model.takePayment(type: .authorization) }
                    Button("Save card") { model.saveCard() }
                    Button("Transaction list") { model.loadTransactions() }
                    Button("Full refund") { model.fullRefund() }
                    Button("Partial refund") { model.partialRefund() }
                    Button("Capture authorization") { model.capture() }
                    Button("Clear-data read") { model.clearDataRead() }
                    Button("Reboot reader") { model.rebootReader() }
                    Button("Cancel pending") { model.cancelPending() }
                }

                Section(header: Text("Result")) {
                    Text(model.statusText)
                        .font(.body)
                }
            }
            .navigationBarTitle("BluePOS Go", displayMode: .inline)
        }
        .navigationViewStyle(StackNavigationViewStyle())
    }
}
