<?php
/**
 * PayConex Apple Pay merchant registration + enrollment client.
 *
 * Endpoints:
 *   POST {v4.api_url}/api/v1/apple_pay/merchant_registration/{account_id}
 *   GET  {v4.api_url}/api/v4/accounts/{account_id}/applePay/enrollment
 *
 * Docs:
 * https://developers.bluefin.com/payconex/docs/getting-started-1
 */

declare(strict_types=1);

final class MerchantRegistration
{
    public function __construct(
        private readonly string $apiBaseUrl,
        private readonly string $accountId,
        private readonly PayConexHmac $hmac,
    ) {
    }

    /**
     * @param list<string> $domains
     * @return array{
     *   ok: bool,
     *   http_status: int,
     *   endpoint: string,
     *   method: string,
     *   request_body: array<string, mixed>|null,
     *   hmac: array<string, string>,
     *   response_raw: string,
     *   response_json: mixed,
     *   curl_error: string|null
     * }
     */
    public function register(
        array $domains,
        string $merchantUrl,
        string $partnerMerchantName,
    ): array {
        # $path = '/api/v1/apple_pay/merchant_registration/' . rawurlencode($this->accountId);
        $path = '/api/v4/accounts/' . rawurlencode($this->accountId) . '/applePay/enrollment';

        $body = [
            'domains'             => array_values($domains),
            'merchantUrl'         => $merchantUrl,
            # 'partnerMerchantName' => $partnerMerchantName,
        ];

        // Encode once and reuse so the ContentHash matches the bytes we send.
        $rawBody = json_encode($body, JSON_UNESCAPED_SLASHES);
        if ($rawBody === false) {
            throw new RuntimeException('Failed to encode merchant registration body as JSON.');
        }

        return $this->request('POST', $path, $rawBody, $body);
    }

    /**
     * Fetch domains already enrolled for Apple Pay on this account.
     *
     * @return array{
     *   ok: bool,
     *   http_status: int,
     *   endpoint: string,
     *   method: string,
     *   request_body: null,
     *   hmac: array<string, string>,
     *   response_raw: string,
     *   response_json: mixed,
     *   curl_error: string|null
     * }
     */
    public function getEnrollment(): array
    {
        $path = '/api/v4/accounts/' . rawurlencode($this->accountId) . '/applePay/enrollment';

        return $this->request('GET', $path, '', null);
    }

    /**
     * @param array<string, mixed>|null $requestBody
     * @return array{
     *   ok: bool,
     *   http_status: int,
     *   endpoint: string,
     *   method: string,
     *   request_body: array<string, mixed>|null,
     *   hmac: array<string, string>,
     *   response_raw: string,
     *   response_json: mixed,
     *   curl_error: string|null
     * }
     */
    private function request(
        string $method,
        string $path,
        string $rawBody,
        ?array $requestBody,
    ): array {
        $url = rtrim($this->apiBaseUrl, '/') . $path;
        $signed = $this->hmac->sign($method, $path, $rawBody);

        $ch = curl_init($url);
        if ($ch === false) {
            throw new RuntimeException('Failed to initialize cURL.');
        }

        $headers = [
            'Accept: application/json',
            'Authorization: ' . $signed['authorization'],
        ];

        $opts = [
            CURLOPT_CUSTOMREQUEST  => strtoupper($method),
            CURLOPT_RETURNTRANSFER => true,
            CURLOPT_TIMEOUT        => 30,
            CURLOPT_HTTPHEADER     => $headers,
        ];

        if (strtoupper($method) !== 'GET') {
            $headers[] = 'Content-Type: application/json';
            $opts[CURLOPT_HTTPHEADER] = $headers;
            $opts[CURLOPT_POSTFIELDS] = $rawBody;
        }

        curl_setopt_array($ch, $opts);

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

        return [
            'ok'           => $curlError === null && $httpStatus >= 200 && $httpStatus < 300,
            'http_status'  => $httpStatus,
            'endpoint'     => $url,
            'method'       => strtoupper($method),
            'request_body' => $requestBody,
            // Never return the API secret; only non-secret HMAC debug fields.
            'hmac'         => [
                'authorization' => $signed['authorization'],
                'nonce'         => $signed['nonce'],
                'timestamp'     => $signed['timestamp'],
                'content_hash'  => $signed['content_hash'],
                'string_to_hash_escaped' => str_replace("\n", '\\n', $signed['string_to_hash']),
            ],
            'response_raw' => $responseRaw,
            'response_json'=> $responseJson,
            'curl_error'   => $curlError,
        ];
    }
}
