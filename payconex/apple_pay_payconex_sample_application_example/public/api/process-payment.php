<?php
/**
 * POST /api/process-payment.php
 *
 * Accepts an Apple Pay payment token from the browser and submits a
 * QSAPI SALE with tender_type=APay.
 *
 * Expected JSON body:
 * {
 *   "amount": "1.00",
 *   "token": { ... Apple Pay payment.token object ... }
 * }
 *
 * Docs:
 * https://developers.bluefin.com/payconex/docs/using-apple-pay-token
 */

declare(strict_types=1);

require_once dirname(__DIR__, 2) . '/src/bootstrap.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    json_error('Method not allowed. Use POST.', 405);
}

$config = app_config();
$input = read_json_body();

$amount = trim((string) ($input['amount'] ?? ''));
if ($amount === '' || !preg_match('/^\d+(\.\d{1,2})?$/', $amount)) {
    json_error('amount must be a decimal string such as "1.00".');
}

$token = $input['token'] ?? null;
if (!is_array($token)) {
    json_error('token must be the Apple Pay payment.token object.');
}

// Some browsers/SDKs deliver paymentData as a JSON string — normalize first.
if (isset($token['paymentData']) && is_string($token['paymentData'])) {
    $decodedPaymentData = json_decode($token['paymentData'], true);
    if (!is_array($decodedPaymentData)) {
        json_error('token.paymentData was a string but not valid JSON.');
    }
    $token['paymentData'] = $decodedPaymentData;
}

// Prefer the full token (paymentData + paymentMethod + transactionIdentifier)
// as shown in Bluefin's QSAPI example. If the client already sent paymentData
// fields at the top level, wrap/pass through sensibly.
if (isset($token['paymentData']) && is_array($token['paymentData'])) {
    $apayPayload = $token;
} elseif (isset($token['data'], $token['signature'], $token['header'])) {
    // Client sent paymentData fields directly.
    $apayPayload = ['paymentData' => $token];
} else {
    json_error('token is missing paymentData (or paymentData fields).');
}

$qsapi = $config['qsapi'] ?? [];
$accountId = (string) ($qsapi['account_id'] ?? '');
$accessKey = (string) ($qsapi['api_accesskey'] ?? '');
$apiUrl = (string) ($qsapi['api_url'] ?? '');

if ($accountId === '' || $accessKey === '' || $apiUrl === '') {
    json_error('Incomplete QSAPI credentials in config.php.', 500);
}

try {
    $client = new QsapiClient($apiUrl, $accountId, $accessKey);
    $result = $client->sale($amount, $apayPayload);
} catch (Throwable $e) {
    json_error('QSAPI request failed: ' . $e->getMessage(), 500);
}

json_ok([
    'ok'       => $result['ok'],
    'approved' => $result['approved'],
    'result'   => $result,
]);
