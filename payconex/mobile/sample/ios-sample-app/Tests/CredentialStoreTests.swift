import Foundation

@main
struct CredentialStoreTests {
    static func main() throws {
        let values = [
            "BLUEPOS_ACCOUNT_ID": "test-account",
            "BLUEPOS_API_KEY": "user",
            "BLUEPOS_API_SECRET": "pass"
        ]
        let credentials = try CredentialStore(values: values)
        precondition(credentials.basicToken == "Basic dXNlcjpwYXNz")
        precondition(credentials.accountId == "test-account")
        precondition(credentials.environment == "STAGING")

        var changed = values
        changed["BLUEPOS_ENVIRONMENT"] = " cert \n"
        let cert = try CredentialStore(values: changed)
        precondition(cert.environment == "CERT")
        changed["BLUEPOS_ENVIRONMENT"] = "PROD"
        let prod = try CredentialStore(values: changed)
        precondition(prod.environment == "PROD")

        for token in ["dXNlcjpwYXNz", "Basic dXNlcjpwYXNz", "  basic dXNlcjpwYXNz\n"] {
            let direct = try CredentialStore(values: [
                "BLUEPOS_ACCOUNT_ID": "test-account",
                "BLUEPOS_BASIC_TOKEN": token
            ])
            precondition(direct.basicToken == "Basic dXNlcjpwYXNz")
        }
        changed = values
        changed["BLUEPOS_BASIC_TOKEN"] = "Basic dG9rZW46b3ZlcnJpZGU="
        let override = try CredentialStore(values: changed)
        precondition(override.basicToken == "Basic dG9rZW46b3ZlcnJpZGU=")

        changed = values
        changed["BLUEPOS_ACCOUNT_ID"] = " "
        try expect(.missingValues(["BLUEPOS_ACCOUNT_ID"])) { try CredentialStore(values: changed) }
        changed = values
        changed["BLUEPOS_API_SECRET"] = ""
        try expect(.missingValues(["BLUEPOS_API_SECRET (or BLUEPOS_BASIC_TOKEN)"])) {
            try CredentialStore(values: changed)
        }
        changed = values
        changed["BLUEPOS_ENVIRONMENT"] = "invalid-environment"
        try expect(.invalidEnvironment) { try CredentialStore(values: changed) }
        try expect(.missingFile) { try CredentialStore.load(from: nil) }

        let directory = FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString)
        try FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
        defer { try? FileManager.default.removeItem(at: directory) }
        let file = directory.appendingPathComponent("payment.plist")
        try Data("not a property list".utf8).write(to: file)
        try expect(.invalidFile) { try CredentialStore.load(from: file) }
        let invalidTypes = try PropertyListSerialization.data(fromPropertyList: ["BLUEPOS_ACCOUNT_ID": 123], format: .xml, options: 0)
        try invalidTypes.write(to: file)
        try expect(.invalidFile) { try CredentialStore.load(from: file) }
        let validData = try PropertyListSerialization.data(fromPropertyList: values, format: .xml, options: 0)
        try validData.write(to: file)
        let loaded = try CredentialStore.load(from: file)
        precondition(loaded.basicToken == "Basic dXNlcjpwYXNz")

        // A configuration failure must never echo the provided credential values.
        changed = values
        changed["BLUEPOS_ACCOUNT_ID"] = ""
        changed["BLUEPOS_API_SECRET"] = "SECRET-SENTINEL"
        do {
            _ = try CredentialStore(values: changed)
            preconditionFailure("Expected a missing account error")
        } catch {
            precondition(!error.localizedDescription.contains("SECRET-SENTINEL"))
        }
        print("Credential configuration checks passed (API keys, token override, environments, file loading and error redaction).")
    }

    static func expect(_ expected: CredentialStore.ConfigurationError, body: () throws -> CredentialStore) throws {
        do {
            _ = try body()
            preconditionFailure("Expected configuration failure")
        } catch let error as CredentialStore.ConfigurationError {
            precondition(error == expected, "Unexpected configuration failure")
        }
    }
}
