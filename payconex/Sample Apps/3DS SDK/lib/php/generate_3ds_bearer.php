<?php

require '../../vendor/autoload.php';

use GuzzleHttp\Client;

define("ACCOUNT_ID", "<ACCOUNT_ID>");
define("API_KEY", "<API_KEY>");

function generate3DSBearer(string $accountId, string $apiKey, string $apiHost) {
    if (empty($accountId)) {
        return "";
    }

    $clientOpts = [
        'headers' => [
            'Accept' => 'application/json',
            'Authorization' => "Basic " . $apiKey
        ]
    ];

    $httpClient = new Client($clientOpts);

    try {
        $response = $httpClient->post(
            "$apiHost/api/v4/accounts/$accountId/auth/token",
            [
                'json' => [
                    "timeout" => 1800,
                    "scopes" => ["pcx:three_d_secure:*"]
                ],
            ]
        );

        $data = json_decode($response->getBody()->getContents(), true);
	echo json_encode($data);

    } catch (\Throwable $th) {
	    echo $th;

    }
}

generate3DSBearer(ACCOUNT_ID, API_KEY, "https://api-cert.payconex.net");

?>
