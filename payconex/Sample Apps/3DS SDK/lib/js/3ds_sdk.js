/* Function to handle submission of the 3DS and PayConex transaction */
async function submitPayment(){

  var payConexRequestParams = {};


  // Transaction Data 
  payConexRequestParams.first_name = document.getElementById('first_name').value;
  payConexRequestParams.last_name = document.getElementById('last_name').value;
  payConexRequestParams.phone = document.getElementById('phone').value;
  payConexRequestParams.email = document.getElementById('email').value;
  payConexRequestParams.street_address1 = document.getElementById('street_address1').value;
  payConexRequestParams.street_address2 = document.getElementById('street_address2').value;
  payConexRequestParams.city = document.getElementById('city').value;
  payConexRequestParams.state = document.getElementById('state').value;
  payConexRequestParams.zip = document.getElementById('zip').value;
  payConexRequestParams.transaction_amount = document.getElementById('transaction_amount').value;

  // Card Data
  payConexRequestParams.card_number = document.getElementById('card_number').value;
  payConexRequestParams.card_verification = document.getElementById('_cvv').value;
  payConexRequestParams.card_expiration = document.getElementById('card_expiry').value;
  
  let bearer_token = await generate3DSBearer()
  
  let { threeDSecure } = await process3DS(bearer_token)
  
  // 3DS Data
  payConexRequestParams.ddds_dstransid = threeDSecure.dsTransId
  payConexRequestParams.ddds_authenticationvalue = threeDSecure.authenticationValue
  payConexRequestParams.ddds_eci = threeDSecure.eci
  payConexRequestParams.ddds_status = threeDSecure.status
  payConexRequestParams.ddds_version = threeDSecure.version

  console.log("payConexRequest Params: ", payConexRequestParams)
  
  await payConexRequest(payConexRequestParams);
  

  return false;
}

//Function to POST form data to server for processing via PayConex.
async function payConexRequest(data) {
  
  let res = await fetch("lib/php/process.php", {
    method: "POST",
    headers: {
      "Content-Type": "application/x-www-form-urlencoded",
    },
    body: new URLSearchParams(data),
  })
  
  data = await res.json()
  
  document.getElementById('request-header').innerHTML = data.requestHeader;

  document.getElementById('request-body').innerHTML = data.requestBody;

  document.getElementById('payconex-response').innerHTML = JSON.stringify(data.response, null, 2);

  document.getElementById('curl-example').innerHTML = data.curlExample;
}

async function generate3DSBearer() {
  let res = await fetch("lib/php/generate_3ds_bearer.php", {
    method: "POST",
  })
  
  const { bearer: bearer_token } = await res.json()
  
  return bearer_token
}

function getCompactTimestamp() {
  const now = new Date();

  const year = now.getFullYear();
  const month = String(now.getMonth() + 1).padStart(2, '0'); // Months are 0-indexed
  const day = String(now.getDate()).padStart(2, '0');
  const hour = String(now.getHours()).padStart(2, '0');
  const minute = String(now.getMinutes()).padStart(2, '0');
  const second = String(now.getSeconds()).padStart(2, '0');

  return `${year}${month}${day}${hour}${minute}${second}`;
}

async function process3DS(bearer_token) {
    const first_name = document.getElementById('first_name').value;
    const last_name = document.getElementById('last_name').value;
    const phone = document.getElementById('phone').value;
    const email = document.getElementById('email').value;
        
    const zip = document.getElementById('zip').value
    const country = document.getElementById('country').value
    const company = document.getElementById('company').value
    const state = document.getElementById('state').value
    const city = document.getElementById('city').value
    
    const address1 = document.getElementById('street_address1').value
    const address2 = document.getElementById('street_address2').value
    
    const amount = document.getElementById('transaction_amount').value;
    
    const card_number = document.getElementById('card_number').value;
    const card_expiration = document.getElementById('card_expiry').value;
    const card_verification = document.getElementById('_cvv').value;
    
    
    let data = {
        "card": {
            "pan": card_number,
            "expiry": card_expiration,
            "cvv": card_verification
        },
        "purchase": {
            "currency": 'USD',
            "date": getCompactTimestamp(),
            "amount": amount,
            "reorderIndicator": "FIRST_TIME_ORDERED"
        },
        "challengeIndicator": "PREFER_A_CHALLENGE",
        "customer": {
            "name": first_name + ' ' + last_name,
            "email": email,
            "phone": phone,
            "billingAddress": {
                "address1": address1,
                "address2": address2,
                "city": city,
                "state": state,
                "zip": zip,
                "country": country,
                "company": company
            }
        },
        "shipping": {
            "indicator": "BILLING_ADDRESS",
            "address": {
                "address1": address1,
                "address2": address2,
                "city": city,
                "state": state,
                "zip": zip,
                "country": country,
                "company": company
            }
        }
    }
    
    const accountId = '<accountId>';
    const iFrameId = 'my_iframe_d';
    
    const apiUrl = "https://api-cert.payconex.net/api/v4";
    
    const sdkOptions = {};

    const bluefin3DSSDK = new Bluefin3DSSDK(bearer_token, accountId, iFrameId, apiUrl, sdkOptions);
    
    const threeDS_data = await bluefin3DSSDK.init(data.card, data.purchase, data.challengeIndicator, data.customer, data.shipping);
    
    document.getElementById('3ds-response').innerHTML = JSON.stringify(threeDS_data, null, 2);
    
    return threeDS_data;
}
