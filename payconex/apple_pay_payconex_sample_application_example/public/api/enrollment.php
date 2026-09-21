<?php
/**
 * GET /api/enrollment.php
 *
 * Returns Apple Pay enrollment details for the configured account
 * (domains already registered with PayConex).
 *
 * PayConex:
 *   GET /api/v4/accounts/{accountId}/applePay/enrollment
 */

declare(strict_types=1);

require_once dirname(__DIR__, 2) . '/src/bootstrap.php';

if ($_SERVER['REQUEST_METHOD'] !== 'GET') {
    json_error('Method not allowed. Use GET.', 405);
}

$config = app_config();
$v4 = $config['v4'] ?? [];
$qsapi = $config['qsapi'] ?? [];

$accountId = (string) ($qsapi['account_id'] ?? '');
$apiKeyId = (string) ($v4['api_key_id'] ?? '');
$apiKeySecret = (string) ($v4['api_key_secret'] ?? '');
$apiUrl = (string) ($v4['api_url'] ?? '');

if ($accountId === '' || $apiKeyId === '' || $apiKeySecret === '' || $apiUrl === '') {
    json_error('Incomplete v4/QSAPI credentials in config.php.', 500);
}

try {
    $hmac = new PayConexHmac($apiKeyId, $apiKeySecret);
    $client = new MerchantRegistration($apiUrl, $accountId, $hmac);
    $result = $client->getEnrollment();
} catch (Throwable $e) {
    json_error('Enrollment lookup failed: ' . $e->getMessage(), 500);
}

$enrollment = is_array($result['response_json']) ? $result['response_json'] : [];
$domains = [];
if (isset($enrollment['domains']) && is_array($enrollment['domains'])) {
    foreach ($enrollment['domains'] as $domain) {
        if (is_string($domain) && $domain !== '') {
            $domains[] = $domain;
        }
    }
}

json_ok([
    'ok'     => $result['ok'],
    'result' => $result,
    'enrollment' => [
        'domains'            => $domains,
        'payconexAccountId'  => $enrollment['payconexAccountId'] ?? $accountId,
        'displayName'        => $enrollment['displayName'] ?? null,
    ],
]);
