const CryptoJS = require('crypto-js')

const accountId = process.env.ACCOUNT_ID
const SECRET = process.env.SECRET // API Key Secret
const API_KEY_ID = process.env.API_KEY_ID
const PAYCONEX_TOKEN = process.env.PAYCONEX_TOKEN
const URL = `/api/v4/accounts/${accountId}/payments/sale`


console.log(URL)

const request = { method: 'post', url: URL, data: `{
  "bfTokenReference": "${PAYCONEX_TOKEN}",
  "posProfile": "MOTO",
  "amounts": { "total": "5", "currency": "USD" }
}` }

const method = request.method.toUpperCase()
const url = URL
const timestamp = Math.round(+new Date() / 1000)
const nonce = CryptoJS.lib.WordArray.random(32)

const body = JSON.stringify(JSON.parse(request.data))

const payloadHash = CryptoJS.SHA256(body)

const canonicalRequest = ""
    + method + " " + url + "\n"
    + nonce + "\n"
    + timestamp + "\n"
    + "\n"
    + payloadHash

const digest = CryptoJS.HmacSHA256(canonicalRequest, SECRET)
const apiId = API_KEY_ID

const authHeader = "Hmac "
    + "id=\"" + apiId + "\""
    + ", nonce=\"" + nonce + "\""
    + ", timestamp=\"" + timestamp + "\""
    + ", response=\"" + digest + "\""

console.log('timestamp:        ' + timestamp)
console.log('nonce:            ' + nonce  )
console.log('body:             ' + body   ) // Carefully, take this body exactly as it is and put in your API call. Otherwise, you will receive an authentication required error message
console.log('SHA256:           ' + payloadHash   )
console.log('SHA 1 :           ' + CryptoJS.SHA1(body)   )
console.log('canonicalRequest: ' + canonicalRequest)
console.log('response:         ' + digest )
console.log('authHeader:       ' + authHeader)

// This will be the generated header used in your request configuration
// i.e. 'Authorization: Hmac username="WATERFORD", nonce="1l5daa1ju1b7lmljc5p4nev0ve",  timestamp=1489574949, response="7fd904ec88c5dc9217e178bc8e115b950c243197b5116e3e1fc43061eeb846ac"'
console.log('Authorization: ' + authHeader)

