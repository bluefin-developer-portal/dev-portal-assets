<?php
/**
 * Shared bootstrap for API endpoints and PHP page wrappers.
 *
 * Resolves paths relative to the repository root so the same code works
 * when DocumentRoot is `public/` (Docker) or a cPanel layout where
 * `public/`, `src/`, and `config.php` sit under the site root.
 */

declare(strict_types=1);

/**
 * Absolute path to the project root (parent of `src/` and `public/`).
 */
function app_root(): string
{
    return dirname(__DIR__);
}

/**
 * Load config.php (required). Returns the config array.
 *
 * @return array<string, mixed>
 */
function app_config(): array
{
    static $config = null;

    if ($config !== null) {
        return $config;
    }

    $path = app_root() . '/config.php';
    if (!is_readable($path)) {
        http_response_code(500);
        header('Content-Type: application/json; charset=utf-8');
        echo json_encode([
            'ok'    => false,
            'error' => 'config.php is missing. Copy config.example.php to config.php and add your PayConex credentials.',
        ]);
        exit;
    }

    /** @var array<string, mixed> $loaded */
    $loaded = require $path;
    $config = $loaded;

    return $config;
}

/**
 * Autoload the small set of src/ classes used by this demo.
 */
spl_autoload_register(static function (string $class): void {
    $map = [
        'PayConexHmac'          => 'PayConexHmac.php',
        'MerchantRegistration'  => 'MerchantRegistration.php',
        'QsapiClient'           => 'QsapiClient.php',
    ];

    if (!isset($map[$class])) {
        return;
    }

    $file = app_root() . '/src/' . $map[$class];
    if (is_readable($file)) {
        require_once $file;
    }
});

/**
 * Read the raw JSON request body as an associative array.
 *
 * @return array<string, mixed>
 */
function read_json_body(): array
{
    $raw = file_get_contents('php://input');
    if ($raw === false || trim($raw) === '') {
        return [];
    }

    $decoded = json_decode($raw, true);
    if (!is_array($decoded)) {
        json_error('Request body must be valid JSON.', 400);
    }

    return $decoded;
}

/**
 * Send a JSON success response and exit.
 *
 * @param array<string, mixed> $payload
 */
function json_ok(array $payload, int $status = 200): never
{
    http_response_code($status);
    header('Content-Type: application/json; charset=utf-8');
    header('Cache-Control: no-store');
    echo json_encode($payload, JSON_UNESCAPED_SLASHES | JSON_PRETTY_PRINT);
    exit;
}

/**
 * Send a JSON error response and exit.
 *
 * @param array<string, mixed> $extra
 */
function json_error(string $message, int $status = 400, array $extra = []): never
{
    http_response_code($status);
    header('Content-Type: application/json; charset=utf-8');
    header('Cache-Control: no-store');
    echo json_encode(array_merge([
        'ok'    => false,
        'error' => $message,
    ], $extra), JSON_UNESCAPED_SLASHES | JSON_PRETTY_PRINT);
    exit;
}

/**
 * Non-secret values safe to embed in browser pages.
 *
 * @return array<string, mixed>
 */
function public_config(): array
{
    $config = app_config();

    return [
        'account_id'    => (string) ($config['qsapi']['account_id'] ?? ''),
        'display_name'  => (string) ($config['apple_pay']['display_name'] ?? 'Merchant'),
        'default_domain'=> (string) ($config['apple_pay']['default_domain'] ?? ''),
        'merchant_url'  => (string) ($config['apple_pay']['default_merchant_url'] ?? ''),
        'partner_name'  => (string) ($config['apple_pay']['partner_merchant_name'] ?? ''),
        'default_amount'=> (string) ($config['apple_pay']['default_amount'] ?? '1.00'),
        'apple_merchant_id' => (string) ($config['apple_pay']['apple_merchant_id'] ?? ''),
        // Used by checkout.js to call PayConex applePay/session from the browser.
        'v4_api_host'   => (string) ($config['v4']['api_url'] ?? ''),
    ];
}
