<?php
//API authentication values.
define("ACCOUNT_ID", "<ACCOUNT_ID>");
define("API_ACCESSKEY", "<API_ACCESSKEY>");
 
//Function to POST to PayConex API (QSAPI)
function curlRequest($params){
    $params['account_id'] = ACCOUNT_ID;
    $params['api_accesskey'] = API_ACCESSKEY;
    $params['response_format'] = 'JSON';
    $params['transaction_type'] = 'SALE';
    $params['tender_type'] = 'CARD';
    
    $url = "https://cert.payconex.net/api/qsapi/3.8";
    
    $ch = curl_init($url);

    //The URL to fetch.
    curl_setopt($ch, CURLOPT_URL, $url);

    //  The full data to post in a HTTP "POST" operation.
    $query = http_build_query($params);

    //echo $query;
    curl_setopt($ch, CURLOPT_POSTFIELDS, $query);

    //FALSE to stop cURL from verifying the peer's certificate.
    curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, 0);

    //Set SSL version to TLS 1.2.
    curl_setopt ($ch, CURLOPT_SSLVERSION, 6);

    //TRUE to return the transfer as a string of the return value of curl_exec() instead of outputting it out directly.
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, TRUE);
    curl_setopt($ch, CURLOPT_POST, TRUE);
    curl_setopt($ch, CURLINFO_HEADER_OUT, TRUE);
    
    //The response from RSAPI
    $res = curl_exec($ch);
    if ($res === false){
        throw new Exception('PHP Curl error - ' . curl_error($ch));
    }
    else {
        $info = curl_getinfo($ch);
        //Create example cURL request.
        $curlRequestExample = 'curl -d "'.$query.'" -H "Content-Type: application/x-www-form-urlencoded" -X POST '.$url;

        //Add request/response values for response back to the client-side.
        $response = array(
            'curlExample' => $curlRequestExample,
            'requestHeader' => $info['request_header'],
            'requestBody' => "Body: ".$query,
            'response' => json_decode($res, true)
        );

        //echo JSON encoded response array.
        echo json_encode($response,true);
    }
}

//get POST data
$request = $_POST;

//Send cURL request
curlRequest($request);

?>
