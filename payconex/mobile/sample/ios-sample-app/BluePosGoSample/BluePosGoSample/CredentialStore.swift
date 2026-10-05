import Foundation

/// Immutable, validated sample configuration loaded from the app-bundled payment.plist.
/// Despite the name, this is a parser/adapter, not Keychain storage. The ignored source plist
/// contains credentials recoverable from the built app; choose suitable credential
/// provisioning for a distributed merchant app. Never commit real values to example files.
/// Foundation-only dependencies let Tests/CredentialStoreTests.swift run without the SDK/UI.
struct CredentialStore {
    let basicToken: String
    let accountId: String
    let environment: String

    // Errors identify missing/invalid keys rather than echoing secret values into the UI.
    enum ConfigurationError: LocalizedError, Equatable {
        case missingFile
        case invalidFile
        case missingValues([String])
        case invalidEnvironment

        var errorDescription: String? {
            let instruction = "Update payment.plist and rebuild the app."
            switch self {
            case .missingFile:
                return "Payment configuration is missing. Copy payment.example.plist to payment.plist and rebuild the app."
            case .invalidFile:
                return "Payment configuration must be a valid plist with string values. \(instruction)"
            case .missingValues(let names):
                return "Missing payment configuration: \(names.joined(separator: ", ")). \(instruction)"
            case .invalidEnvironment:
                return "BLUEPOS_ENVIRONMENT must be STAGING, CERT or PROD. \(instruction)"
            }
        }
    }

    /// Require a dictionary of string values; missing files and malformed/non-string plists
    /// produce distinct configuration errors. No fallback credentials or network lookup is used.
    static func load(from url: URL?) throws -> CredentialStore {
        guard let url else { throw ConfigurationError.missingFile }
        guard let data = try? Data(contentsOf: url),
              let values = try? PropertyListDecoder().decode([String: String].self, from: data) else {
            throw ConfigurationError.invalidFile
        }
        return try CredentialStore(values: values)
    }

    /// Normalize surrounding whitespace and environment spelling. Blank environment defaults
    /// to STAGING. A non-empty BLUEPOS_BASIC_TOKEN overrides the API key/secret pair; account ID
    /// is still required. Validation here checks structure/presence, not server authentication.
    init(values: [String: String]) throws {
        func value(_ key: String) -> String {
            (values[key] ?? "").trimmingCharacters(in: .whitespacesAndNewlines)
        }

        let accountId = value("BLUEPOS_ACCOUNT_ID")
        let configuredEnvironment = value("BLUEPOS_ENVIRONMENT").uppercased()
        let environment = configuredEnvironment.isEmpty ? "STAGING" : configuredEnvironment
        let apiKey = value("BLUEPOS_API_KEY")
        let apiSecret = value("BLUEPOS_API_SECRET")
        var suppliedToken = value("BLUEPOS_BASIC_TOKEN")
        // Accept either the encoded credential or its full Basic header value.
        if let prefix = suppliedToken.split(maxSplits: 1, whereSeparator: { $0.isWhitespace }).first,
           prefix.caseInsensitiveCompare("Basic") == .orderedSame {
            suppliedToken = String(suppliedToken.dropFirst(prefix.count))
                .trimmingCharacters(in: .whitespacesAndNewlines)
        }

        var missing: [String] = []
        if accountId.isEmpty { missing.append("BLUEPOS_ACCOUNT_ID") }
        if suppliedToken.isEmpty {
            if apiKey.isEmpty { missing.append("BLUEPOS_API_KEY (or BLUEPOS_BASIC_TOKEN)") }
            if apiSecret.isEmpty { missing.append("BLUEPOS_API_SECRET (or BLUEPOS_BASIC_TOKEN)") }
        }
        guard missing.isEmpty else { throw ConfigurationError.missingValues(missing) }
        guard ["STAGING", "CERT", "PROD"].contains(environment) else {
            throw ConfigurationError.invalidEnvironment
        }

        self.accountId = accountId
        self.environment = environment
        // Basic is Base64 of UTF-8 "API key ID:API secret", not encryption and not the website
        // password. Supplied tokens are normalized but not decoded/authenticated by this parser.
        self.basicToken = suppliedToken.isEmpty
            ? "Basic " + Data("\(apiKey):\(apiSecret)".utf8).base64EncodedString()
            : "Basic " + suppliedToken
    }
}

/// Callback addresses must match the URL scheme registered in Info.plist. When adapting the
/// sample, change BOTH the scheme here and its registration (plus the developer-panel hint).
/// A custom URL scheme routes an app return; it is not an authentication/security boundary.
/// Always let BluePosGo.handleCallback parse/match the vendor result before changing order state.
enum CallbackPath {
    static let scheme = "mymerchantapp"

    // Callers supply fixed operation paths, not user input. The force unwrap relies on that
    // invariant; use a failable/validated URL builder if paths become externally supplied.
    static func url(_ path: String) -> URL {
        URL(string: "\(scheme)://bluepos/\(path)")!
    }
}
