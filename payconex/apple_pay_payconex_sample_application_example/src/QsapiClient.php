<?php
/**
 * Thin QSAPI client for Apple Pay SALE transactions.
 *
 * Important fields when charging with an Apple Pay token:
 *   - tender_type       = APay
 *   - transaction_type  = SALE
 *   - apay_payload      = JSON for the Apple Pay token / paymentData object
 *
 * Docs:
 * https://developers.bluefin.com/payconex/docs/using-apple-pay-token
 */

declare(strict_types=1);

final class QsapiClient
{
    public function __construct(
        private readonly string $apiUrl,
        private readonly string $accountId,
        private readonly string $apiAccessKey,
    ) {
    }

    /**
     * Process an Apple Pay SALE.
     *
     * @param array<string, mixed> $apayPayload Apple Pay token (or paymentData) payload
     * @return array{
     *   ok: bool,
     *   approved: bool,
     *   http_status: int,
     *   endpoint: string,
     *   request_fields: array<string, string>,
     *   response_raw: string,
     *   response_json: mixed,
     *   curl_error: string|null
     * }
     */
    public function sale(string $amount, array $apayPayload): array
    {
        $endpoint = $this->apiUrl;

        // Bluefin's documented example sends the full token object (paymentData,
        // paymentMethod, transactionIdentifier) as apay_payload JSON.
        $apayJson = json_encode($apayPayload, JSON_UNESCAPED_SLASHES);
        if ($apayJson === false) {
            throw new RuntimeException('Failed to encode apay_payload as JSON.');
        }

        $fields = [
            'account_id'         => $this->accountId,
            'api_accesskey'      => $this->apiAccessKey,
            'tender_type'        => 'APay',
            'transaction_type'   => 'SALE',
            'transaction_amount' => $amount,
            'response_format'    => 'JSON',
            'apay_payload'       => $apayJson,
        ];

        $ch = curl_init($endpoint);
        if ($ch === false) {
            throw new RuntimeException('Failed to initialize cURL.');
        }

        curl_setopt_array($ch, [
            CURLOPT_POST           => true,
            CURLOPT_POSTFIELDS     => http_build_query($fields),
            CURLOPT_RETURNTRANSFER => true,
            CURLOPT_TIMEOUT        => 45,
            CURLOPT_HTTPHEADER     => [
                'Content-Type: application/x-www-form-urlencoded',
                'Accept: application/json',
            ],
        ]);

        $responseRaw = curl_exec($ch);
        $curlError = curl_error($ch) ?: null;
        $httpStatus = (int) curl_getinfo($ch, CURLINFO_HTTP_CODE);
        curl_close($ch);

        if ($responseRaw === false) {
            $responseRaw = '';
        }

        $responseJson = json_decode($responseRaw, true);
        if ($responseJson === null && $responseRaw !== '' && $responseRaw !== 'null') {
            $responseJson = $responseRaw;
        }

        // Redact the access key from anything we echo back to the UI.
        $safeFields = $fields;
        $safeFields['api_accesskey'] = '[redacted]';
        $safeFields['apay_payload'] = '[Apple Pay token JSON — omitted from debug panel]';

        $approved = false;
        if (is_array($responseJson)) {
            $code = (string) ($responseJson['error_code'] ?? $responseJson['transaction_approved'] ?? '');
            // QSAPI commonly uses error_code "000" / "0" or transaction_approved "1"/"true".
            $approved = ($responseJson['transaction_approved'] ?? null) == 1
                || ($responseJson['transaction_approved'] ?? null) === true
                || ($responseJson['transaction_approved'] ?? null) === '1'
                || $code === '000'
                || $code === '0';
        }

        return [
            'ok'             => $curlError === null && $httpStatus >= 200 && $httpStatus < 300 && $approved,
            'approved'       => $approved,
            'http_status'    => $httpStatus,
            'endpoint'       => $endpoint,
            'request_fields' => $safeFields,
            'response_raw'   => $responseRaw,
            'response_json'  => $responseJson,
            'curl_error'     => $curlError,
        ];
    }
}
