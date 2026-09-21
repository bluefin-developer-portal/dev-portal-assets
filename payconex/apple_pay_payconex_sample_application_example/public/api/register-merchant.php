<?php
/**
 * POST /api/register-merchant.php
 *
 * Registers one or more domains for Apple Pay via the PayConex
 * merchant registration API. HMAC signing happens entirely on the server.
 *
 * Expected JSON body:
 * {
 *   "domains": ["example.com"],
 *   "merchantUrl": "example.com",
 *   "partnerMerchantName": "Example Merchant"
 * }
 */

declare(strict_types=1);

require_once dirname(__DIR__, 2) . '/src/bootstrap.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    json_error('Method not allowed. Use POST.', 405);
}

$config = app_config();
$input = read_json_body();

$domains = $input['domains'] ?? [];
if (!is_array($domains) || $domains === []) {
    json_error('Provide at least one domain in "domains".');
}

$cleanDomains = [];
foreach ($domains as $domain) {
    if (!is_string($domain)) {
        continue;
    }
    $domain = strtolower(trim($domain));
    // Strip scheme if the user pasted a full URL.
    $domain = preg_replace('#^https?://#', '', $domain) ?? $domain;
    $domain = rtrim($domain, '/');
    $domain = preg_replace('#:\d+$#', '', $domain) ?? $domain;
    if ($domain === '' || !preg_match('/^[a-z0-9.-]+$/i', $domain)) {
        json_error('Invalid domain: ' . $domain);
    }
    $cleanDomains[] = $domain;
}
$cleanDomains = array_values(array_unique($cleanDomains));
if ($cleanDomains === []) {
    json_error('Provide at least one valid domain.');
}

$merchantUrl = trim((string) ($input['merchantUrl'] ?? ''));
$partnerName = trim((string) ($input['partnerMerchantName'] ?? ''));

// merchantUrl is a hostname (no scheme / port), same shape PayConex expects here.
$merchantUrl = strtolower($merchantUrl);
$merchantUrl = preg_replace('#^https?://#', '', $merchantUrl) ?? $merchantUrl;
$merchantUrl = rtrim($merchantUrl, '/');
$merchantUrl = preg_replace('#:\d+$#', '', $merchantUrl) ?? $merchantUrl;
$merchantUrl = preg_replace('#/.*$#', '', $merchantUrl) ?? $merchantUrl;

if ($merchantUrl === '' || !preg_match('/^[a-z0-9.-]+$/i', $merchantUrl)) {
    json_error('merchantUrl must be a hostname (e.g. example.com), without https:// or a port.');
}
if ($partnerName === '') {
    json_error('partnerMerchantName is required.');
}

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
    $result = $client->register($cleanDomains, $merchantUrl, $partnerName);
} catch (Throwable $e) {
    json_error('Merchant registration failed: ' . $e->getMessage(), 500);
}

json_ok([
    'ok'     => $result['ok'],
    'result' => $result,
]);
