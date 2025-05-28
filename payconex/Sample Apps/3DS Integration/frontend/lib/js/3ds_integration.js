/* Function to handle submission of the 3DS and PayConex transaction */
async function submitPayment(){

  await process3DS()
  
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

const ACS_MAX_ATTEMPT = 10 // Attempt per RETRY_TIMEOUT value
const threeDS_STATUS_MAX_ATTEMPT = 10 // Attempt per RETRY_TIMEOUT value
const STATUS_RETRY_TIMEOUT = 1000 // 1 second
const ACS_RETRY_TIMEOUT = 1000 // 1 second

function injectIframeHTML(id, HTMLContent) {
  let doc = document.getElementById(id).contentDocument
  doc.open()
  doc.write(HTMLContent)
  doc.close()
}

class threeDSIntegration {

  constructor(card_data, authentication_data) {
    this.ACS_MAX_ATTEMPT = ACS_MAX_ATTEMPT
    this.threeDS_STATUS_MAX_ATTEMPT = threeDS_STATUS_MAX_ATTEMPT

    this.STATUS_RETRY_TIMEOUT = STATUS_RETRY_TIMEOUT
    this.ACS_RETRY_TIMEOUT = ACS_RETRY_TIMEOUT

    this.card_data = card_data
    this.authentication_data = authentication_data

    this.card_init_res = null
    this.acs_done = false

    this.acs_count = 0
    this.threeds_status_count = 0

  }

  #shouldMakeACSCall(initData) {
    if (initData.status === "CARDHOLDER_ACCOUNT_ENROLLED"
      && initData.acsUrl
      && initData.threeDSecureData
      && initData.acsResultUrl) {
      return true
    }
    return false
  }

  async init_card() {

    let res = await fetch('/init-card', {
      method: 'POST',
      headers: {
        "content-type": "application/json",
      },
      body: JSON.stringify(this.card_data),

    })

    this.card_init_res = await res.json()

    console.log('DEBUG card_res:', this.card_init_res)
  }

  async threeDStatus() {

    const threeds_status_id = setInterval(async () => {
    
      let res = await fetch('/3ds-status', {
        method: "POST",
        headers: {
          "content-type": "application/json",
        },
        body: JSON.stringify({
          threeDSecureId: this.card_init_res.threeDSecureId
        })
      })

      let threeDSdata = await res.json()
      if(threeDSdata.status == "PROCESS_DONE") {
        console.log("DEBUG 3DS successfully authenticated", threeDSdata)
        // Authorize and process transaction with 3DS data or Proceed with any other requirements
        document.getElementById('3ds-response').innerHTML = JSON.stringify(threeDSdata, null, 2);
        return clearInterval(threeds_status_id)
      }
      this.threeds_status_count++
      if(this.threeds_status_count == this.threeDS_STATUS_MAX_ATTEMPT) {
        // Render the iframe with challenge expired.
        injectIframeHTML("3ds_challenge_iframe", "<p>3DS Challenge Expired</p>")
        return clearInterval(threeds_status_id)
      }
    }, this.STATUS_RETRY_TIMEOUT) // User-defined interval value. Check every 1 second in this case


  }

  async ACS_Status(initData) {

    const acs_status_id = setInterval(async () => {
      let res = await fetch(initData.acsResultUrl, {
        method: "GET"
      })

      console.log('DEBUG ACS status res: ', res)

      if(res.status == 200 && !this.acs_done) {
        console.log("DEBUG Proceeding with Browser Authentication...")
        this.acs_done = true
        await this.browser_authentication({
          threeDSecureId: this.card_init_res.threeDSecureId,
          ...this.authentication_data
        })
        return clearInterval(acs_status_id)
      }
      // Stop after 10 seconds
      this.acs_count++
      if(this.acs_count == this.ACS_MAX_ATTEMPT) {
        // Render the iframe with ACS Method expired.
        injectIframeHTML("acs_iframe_id", "<p>ACS Method Expired</p>")
        return clearInterval(acs_status_id)
      }
    }, this.ACS_RETRY_TIMEOUT) // User-defined interval value. Check every 1 second in this case


  }

  async #request_challenge(data) {

    function buildRequest(data) {
      let body = {}
      data.challenge.form.inputs.forEach(input => {
        body[input.name] = input.value
      })
      return body
    }

    let res = await fetch(data.challenge.form.action, {
      method: data.challenge.form.method,
      headers: {
        "Content-Type": "application/x-www-form-urlencoded",
      },
      body: new URLSearchParams(buildRequest(data)),
    })

    return await res.text()
  }

  async browser_authentication(authentication_data) {
    authentication_data.card.paymentDetailsReference = this.card_init_res.paymentDetailsReference
    console.log("DEBUG Browser Authentication Data:", authentication_data)
    let res = await fetch('/browser-authenticate', {
      method: 'POST',
      headers: {
        "content-type": "application/json",
      },
      body: JSON.stringify(authentication_data),

    })

    let res_body = await res.json()

    console.log("DEBUG BROWSER AUTHENTICATION RES:", res_body)

    if(res_body.status == "CHALLENGE_REQUIRED") {
      let challenge_request_form = await this.#request_challenge(res_body)
      console.log("DEBUG REQUEST CHALLENGE FORM:", challenge_request_form)
      injectIframeHTML("3ds_challenge_iframe", challenge_request_form)

    }
    
    // Start the 3DS status interval in the background
    this.threeDStatus()

  }

  async process3DS() {
    let card_init_data = await this.init_card()

    if(this.#shouldMakeACSCall(this.card_init_res)) {
      let res = await fetch('/request-acs-form', {
        method: 'POST',
        headers: {
          "content-type": "application/json",
        },
        body: JSON.stringify(this.card_init_res),

      })

      let acs_form = (await res.json()).html

      injectIframeHTML("acs_iframe_id", acs_form)

      // Start the ACS status interval in the background
      this.ACS_Status(this.card_init_res)



    } else {
      await this.browser_authentication({
        threeDSecureId: this.card_init_res.threeDSecureId,
        ...this.authentication_data
      })
    }


  }

}

function getBrowserData() {
  return {
    acceptHeader: navigator.userAgent.includes("Chrome") ? "*/*" : navigator.accept || "*/*",
    javaEnabled: navigator.javaEnabled(),
    colorDepth: screen.colorDepth,
    language: navigator.language || navigator.userLanguage,
    screenWidth: screen.width,
    screenHeight: screen.height,
    timezone: new Date().getTimezoneOffset(),
    userAgent: navigator.userAgent
  };
}

async function process3DS() {
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


  let authentication_data = {
    "card": {
      "paymentDetailsReference": ""
    },
    "browser": getBrowserData(),
    "purchase": {
      "currency": 'USD',
      "date": getCompactTimestamp(),
      "amount": amount,
      "transactionType": "GOODS_SERVICE_PURCHASE",
      "deliveryTimeFrame": "SAME_DAY_SHIPPING"
    },
    "threeDSecureChallengeIndicator": "PREFER_A_CHALLENGE",
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
  
  let threeDS_instance = new threeDSIntegration({
    pan: document.getElementById('card_number').value,
    cvv: document.getElementById('_cvv').value,
    expiry: document.getElementById('card_expiry').value,
  }, authentication_data)

  await threeDS_instance.process3DS()

}
