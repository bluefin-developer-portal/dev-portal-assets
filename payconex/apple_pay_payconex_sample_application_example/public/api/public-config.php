<?php
/**
 * GET /api/public-config.php
 *
 * Returns non-secret configuration for the browser (account id, display
 * name, default domain). Secrets never leave the server.
 */

declare(strict_types=1);

require_once dirname(__DIR__, 2) . '/src/bootstrap.php';

if ($_SERVER['REQUEST_METHOD'] !== 'GET') {
    json_error('Method not allowed. Use GET.', 405);
}

json_ok([
    'ok'     => true,
    'config' => public_config(),
]);
