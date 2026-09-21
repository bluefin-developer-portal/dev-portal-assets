<?php
/**
 * Example configuration for the Apple Pay + PayConex demo.
 *
 * Copy this file to config.php and fill in your PayConex certification
 * (or production) credentials. config.php is gitignored and must never
 * be committed or exposed to the browser.
 *
 * Field mapping (if you keep credentials in a separate notes file):
 *   qsapi.account_id      <- QSAPI account id
 *   qsapi.api_accesskey   <- QSAPI access key
 *   v4.api_key_id         <- v4 API key id
 *   v4.api_key_secret     <- v4 API key secret
 */

declare(strict_types=1);

return [
    /*
     * QSAPI — used to process Apple Pay SALE transactions
     * (tender_type=APay). See:
     * https://developers.bluefin.com/payconex/docs/using-apple-pay-token
     */
    'qsapi' => [
        'account_id'    => 'YOUR_ACCOUNT_ID',
        'api_accesskey' => 'YOUR_QSAPI_ACCESS_KEY',
        // Certification QSAPI endpoint (include trailing slash).
        'api_url'       => 'https://cert.payconex.net/api/qsapi/3.8/',
    ],

    /*
     * PayConex API v4 — used for HMAC-authenticated merchant registration
     * and Apple Pay merchant session creation. See:
     * https://developers.bluefin.com/payconex/docs/getting-started-1
     */
    'v4' => [
        'api_key_id'     => 'YOUR_API_KEY_ID',
        'api_key_secret' => 'YOUR_API_KEY_SECRET',
        'api_url'        => 'https://api-cert.payconex.net',
    ],

    /*
     * Apple Pay demo defaults. Leave domain / merchant URL empty to let the
     * registration UI prefill from the current browser hostname.
     * Values are hostnames only (e.g. applepay.local) — no https:// or port.
     * Local Docker: register applepay.local (see README hosts-file steps).
     */
    'apple_pay' => [
        'display_name'          => 'Apple Pay Demo',
        'partner_merchant_name' => 'Apple Pay Demo',
        'default_domain'        => '',
        'default_merchant_url'  => '',
        // Default SALE amount shown on the checkout page.
        'default_amount'        => '1.00',
        // Optional Apple merchant identifier for applePayCapabilities()
        // (e.g. merchant.com.example). Leave empty to use canMakePayments().
        'apple_merchant_id'     => '',
    ],
];
