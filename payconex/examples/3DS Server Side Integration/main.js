const { JSDOM } = require('jsdom')


function getCompactTimestamp() {
  const now = new Date()

  const year = now.getFullYear()
  const month = String(now.getMonth() + 1).padStart(2, '0')
  const day = String(now.getDate()).padStart(2, '0')
  const hour = String(now.getHours()).padStart(2, '0')
  const minute = String(now.getMinutes()).padStart(2, '0')
  const second = String(now.getSeconds()).padStart(2, '0')

  return `${year}${month}${day}${hour}${minute}${second}`
}


const bearer_token = process.env.PAYCONEX_BEARER_TOKEN

let accountId = process.env.accoundId

let URL = 'https://api-cert.payconex.net'

let visa_card = {
  "pan": "4124939999999990",
  "expiry": "1225",
  "cvv": "555"
}

let amex_card = {
  "pan": "340000111111117",
  "expiry": "1225",
  "cvv": "5555"
}


async function initCardDetails(card_data) {
  let res = await fetch(`${URL}/api/v4/accounts/${accountId}/3DS/init-card-details`, {
    method: "POST",
    headers: {
      "content-type": "application/json",
      "Authorization": `Bearer ${bearer_token}`,
    },
    body: JSON.stringify(card_data),
  })

  return await res.json()
}

let browser_auth_man_challenge = {
  "card": {
    "paymentDetailsReference": ""
  },
  "browser": {
    "acceptHeader": "*/*",
    "javaEnabled": false,
    "colorDepth": 24,
    "language": "en-GB",
    "screenWidth": 1920,
    "screenHeight": 1080,
    "timezone": -120,
    "userAgent": "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Safari/537.36"
  },
  "purchase": {
    "currency": "USD",
    "date": "",
    "amount": "1.00",
    "transactionType": "GOODS_SERVICE_PURCHASE",
    "deliveryTimeFrame": "SAME_DAY_SHIPPING"
  },
  "threeDSecureChallengeIndicator": "REQUIRES_MANDATE_CHALLENGE",
  "customer": {
    "name": "Jane Smith",
    "billingAddress": {
      "address1": "123 N Main St",
      "address2": "123",
      "city": "Tulsa",
      "state": "OK",
      "zip": "12345",
      "country": "USA"
    }
  },
  "shipping": {
    "indicator": "BILLING_ADDRESS"
  }
}

let browser_auth_no_challenge = {
  "card": {
    "paymentDetailsReference": ""
  },
  "browser": {
    "acceptHeader": "*/*",
    "javaEnabled": false,
    "colorDepth": 24,
    "language": "en-GB",
    "screenWidth": 1920,
    "screenHeight": 1080,
    "timezone": -120,
    "userAgent": "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Safari/537.36"
  },
  "purchase": {
    "currency": "USD",
    "date": "",
    "amount": "1.00",
    "transactionType": "GOODS_SERVICE_PURCHASE",
    "deliveryTimeFrame": "SAME_DAY_SHIPPING"
  },
  "threeDSecureChallengeIndicator": "PREFER_NO_CHALLENGE",
  "customer": {
    "name": "Jane Smith",
    "billingAddress": {
      "address1": "123 N Main St",
      "address2": "123",
      "city": "Tulsa",
      "state": "OK",
      "zip": "12345",
      "country": "USA"
    }
  },
  "shipping": {
    "indicator": "BILLING_ADDRESS"
  }
}

async function browser_auth(data, browser_data) {
  browser_data.card.paymentDetailsReference = data.paymentDetailsReference
  browser_data.purchase.date = getCompactTimestamp()

  let res = await fetch(`${URL}/api/v4/accounts/${accountId}/3DS/${data.threeDSecureId}/browser-authenticate`, {
    method: "POST",
    headers: {
      "content-type": "application/json",
      "Authorization": `Bearer ${bearer_token}`,
    },
    body: JSON.stringify(browser_data),
  })

  return await res.json()

}

async function request_challenge(data) {

  console.log('data: ', data)
  let res = await fetch(data.action, {
    method: "POST",
    headers: {
      "Content-Type": "application/x-www-form-urlencoded",
    },
    body: new URLSearchParams({
      creq: data.creq,
      threeDSSessionData: data.threeDSSessionData,
    }),
  })

  return await res.text()
}

async function challenge_complete(data) {
  console.log('challenge_complete data:', data)

  let res = await fetch(`${URL}/api/v4/accounts/${accountId}/3DS/${data.threeDSecureId}/challenge-complete`, {
    method: "POST",
    headers: {
      "Content-Type": "application/x-www-form-urlencoded",
    },
    body: new URLSearchParams({
      creq: data.creq,
      cres: data.cres,
      threeDSSessionData: data.threeDSSessionData,
    }),
  })

  console.log('challenge_complete res status code: ', res.status)

  return await res.text()

}

async function threeDS_status(data) {

  let res = await fetch(`${URL}/api/v4/accounts/${accountId}/3DS/${data.threeDSecureId}/status`, {
    method: "GET",
    headers: {
      "content-type": "application/json",
      "Authorization": `Bearer ${bearer_token}`,
    }
  })

  return await res.json()

}

async function threeDS_mandate_challenge() {
  console.log('-'.repeat(5) + 'threeDS_mandate_challenge' + '-'.repeat(5))
  let init_res = await initCardDetails(amex_card)
  console.log('init_res:', init_res)

  let browser_auth_res = await browser_auth(init_res, browser_auth_man_challenge)

  console.log('browser_auth_res:')
  console.dir(browser_auth_res, { depth: null })

  let requested_challenge = await request_challenge({
    action: browser_auth_res.challenge.form.action,
    creq: browser_auth_res.challenge.form.inputs.filter(v=>v.name=='creq')[0].value,
    threeDSSessionData: browser_auth_res.challenge.form.inputs.filter(v=>v.name=='threeDSSessionData')[0].value,
  })
  
  console.log('requested_challenge:', requested_challenge)

  const dom = new JSDOM(requested_challenge)
  const document = dom.window.document

  const creq = document.querySelector("input[name='creq']").value
  const cres = document.querySelector("input[name='cres']").value
  const threeDSSessionData = document.querySelector("input[name='threeDSSessionData']").value

  let completed_challenge = await challenge_complete({
    threeDSecureId: init_res.threeDSecureId,
    creq,
    cres,
    threeDSSessionData,
  })

  console.log('completed_challenge:', completed_challenge)

  let threeds_status = await threeDS_status(browser_auth_res)

  console.log('3ds_status:', threeds_status)
  console.log('-'.repeat(5) + 'threeDS_mandate_challenge' + '-'.repeat(5))
}


async function threeDS_no_challenge() {
  console.log('-'.repeat(5) + 'threeDS_no_challenge' + '-'.repeat(5))
  let init_res = await initCardDetails(visa_card)
  console.log('init_res:', init_res)

  let browser_auth_res = await browser_auth(init_res, browser_auth_no_challenge)

  console.log('browser_auth_res:')
  console.dir(browser_auth_res, { depth: null })

  let threeds_status = await threeDS_status(browser_auth_res)

  console.log('3ds_status:', threeds_status)
  console.log('-'.repeat(5) + 'threeDS_no_challenge' + '-'.repeat(5))
}

;(async function main() {
  await threeDS_mandate_challenge()
  // await threeDS_no_challenge()

})();



