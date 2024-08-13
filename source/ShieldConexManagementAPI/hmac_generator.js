const CryptoJS = require('crypto-js')

const client = require('./client.json')

const accountId = process.env.ACCOUNT_ID
const SECRET = process.env.SECRET // base-64 Decoded Hmac Secret
const USERNAME = process.env.USERNAME
const PAYCONEX_TOKEN = process.env.PAYCONEX_TOKEN
const URL = '/api/v1/clients'


console.log(URL)

const request = { method: 'POST', url: URL, data: client }

const method = request.method.toUpperCase()
const url = URL
const timestamp = Math.round(+new Date() / 1000)
const nonce = CryptoJS.lib.WordArray.random(32)

const body = JSON.stringify(request.data) // For POST

const payloadHash = CryptoJS.SHA256(body)

const canonicalRequest = ""
    + method + " " + url + "\n"
    + nonce + "\n"
    + timestamp + "\n"
    + "\n"
    + payloadHash

const digest = CryptoJS.HmacSHA256(canonicalRequest, SECRET)


const authHeader = "Hmac "
    + "username=\"" + USERNAME + "\""
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

