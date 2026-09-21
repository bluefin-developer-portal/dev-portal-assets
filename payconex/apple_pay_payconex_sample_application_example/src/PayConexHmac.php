<?php
/**
 * PayConex HMAC Authorization helper (API v4 style).
 *
 * PayConex authenticates certain management endpoints with an
 * `Authorization: Hmac ...` header. Building that header requires:
 *
 *   1. ContentHash  = hex(SHA-256(raw request body))
 *   2. string-to-hash =
 *        HTTP_METHOD + " " + API_PATH + "\n"
 *        + nonce + "\n"
 *        + timestamp + "\n"
 *        + "\n"
 *        + ContentHash
 *   3. response     = hex(HMAC-SHA256(api_key_secret, string-to-hash))
 *   4. Header       = Hmac id="...", nonce="...", timestamp="...", response="..."
 *
 * Reference:
 * https://developers.bluefin.com/payconex/docs/getting-started-1
 * https://developers.bluefin.com/payconex/v4/reference/api-authentication
 *
 * Example string-to-hash (newlines shown as \n for readability):
 *
 * POST /api/v4/accounts/180000000742/applePay/enrollment\nD1O48xR1TDvDDDKS7L8EFsXrzu\n1670806392214\n\n4c43f07b4dbc4f21c32ce3b5518d9645faf6ad9b422069fd116f56d25c229aab
 */

declare(strict_types=1);

final class PayConexHmac
{
    /**
     * @param string $apiKeyId     PayConex API key id (HMAC `id` property)
     * @param string $apiKeySecret PayConex API key secret (HMAC signing key)
     */
    public function __construct(
        private readonly string $apiKeyId,
        private readonly string $apiKeySecret,
    ) {
    }

    /**
     * Build the full Authorization header value (including the "Hmac " prefix).
     *
     * @param string $method      HTTP method, e.g. "POST"
     * @param string $apiPath     Path only, e.g. "/api/v1/apple_pay/merchant_registration/123"
     * @param string $rawBody     Exact JSON body that will be sent
     * @param string|null $nonce  Optional nonce (generated if omitted)
     * @param string|null $timestamp Optional unix timestamp in milliseconds
     *
     * @return array{
     *   authorization: string,
     *   nonce: string,
     *   timestamp: string,
     *   content_hash: string,
     *   string_to_hash: string,
     *   response: string
     * }
     */
    public function sign(
        string $method,
        string $apiPath,
        string $rawBody,
        ?string $nonce = null,
        ?string $timestamp = null,
    ): array {
        $nonce = $nonce ?? $this->createNonce();
        $timestamp = $timestamp ?? (string) (int) floor(microtime(true) * 1000);

        // Step 1 — ContentHash of the exact request body.
        $contentHash = hash('sha256', $rawBody);

        // Step 2 — string-to-hash (note the blank line before ContentHash).
        $stringToHash = strtoupper($method)
            . ' '
            . $apiPath
            . "\n"
            . $nonce
            . "\n"
            . $timestamp
            . "\n"
            . "\n"
            . $contentHash;

        // Step 3 — HMAC-SHA256 of the string-to-hash, keyed with the API secret.
        $response = hash_hmac('sha256', $stringToHash, $this->apiKeySecret);

        // Step 4 — Authorization header.
        $authorization = sprintf(
            'Hmac id="%s", nonce="%s", timestamp="%s", response="%s"',
            $this->apiKeyId,
            $nonce,
            $timestamp,
            $response
        );

        return [
            'authorization'  => $authorization,
            'nonce'          => $nonce,
            'timestamp'      => $timestamp,
            'content_hash'   => $contentHash,
            'string_to_hash' => $stringToHash,
            'response'       => $response,
        ];
    }

    /**
     * Create a 26-character alphanumeric nonce.
     * Nonces must be unique within a 10-minute window.
     */
    public function createNonce(int $length = 26): string
    {
        $alphabet = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789';
        $max = strlen($alphabet) - 1;
        $out = '';
        for ($i = 0; $i < $length; $i++) {
            $out .= $alphabet[random_int(0, $max)];
        }
        return $out;
    }
}
