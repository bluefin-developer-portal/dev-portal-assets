const CryptoJS = require('crypto-js')
const Fs = require('fs')
const crypto = require('crypto')

const client = require('./client.json')
console.log(client)

const privateKey = Fs.readFileSync('private8.pem', 'utf8')


const USERNAME = process.env.USERNAME
const URL = '/api/v1/clients'

function signString(stringToSign) {
  const sign = crypto.createSign('RSA-SHA256');
  sign.update(stringToSign);
  const signature = sign.sign(privateKey, 'hex');

  return signature
}


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

const signature = signString(canonicalRequest)


const authHeader = "Rsa "
    + "username=\"" + USERNAME + "\""
    + ", nonce=\"" + nonce + "\""
    + ", timestamp=\"" + timestamp + "\""
    + ", response=\"" + signature + "\""

console.log('timestamp:        ' + timestamp)
console.log('nonce:            ' + nonce  )
console.log('body:             ' + body   ) // Carefully, take this body exactly as it is and put in your API call. Otherwise, you will receive an authentication required error message
console.log('SHA256:           ' + payloadHash   )
console.log('SHA 1 :           ' + CryptoJS.SHA1(body)   )
console.log('canonicalRequest: ' + canonicalRequest)
console.log('response:         ' + signature )
console.log('authHeader:       ' + authHeader)

// This will be the generated header used in your request configuration
// i.e. Authorization: Rsa username="WATERFORD", nonce="1b09256f6ed430f5d8fed72ab0bc86edc996db1812361efc5802c7d6ed18c41e", timestamp="1723207220", response="14743a0624ce39b4564fd570c91eb85f6e46d86817031c38f5e33caf26697d72b0ba15273c10110acb58cb027aa2e145ae230d596971e9c6c0343fcbf425ee20dadb94c539e3621dae0f494411ae7c1104e525d46498b4ad730136581493f8435965ff06fa53ec6aabeb9c7abe7ca157ab9ad0b742d4916311f73505ed5716292343db09f7a1481de4dfb411d200ceb24e7c54909b41e971b5fe8c82ac1616d2ca3d825d5f3796eb48d393740f938ad1f2bd9e8c54cc808a07dbcf58a2541a44e77dd123793c884ba3d92f15d4d15a7773d60412c1bf18e4c54661a91da0e6a1745de9c9cdc974e2ad65a2e8f2982281f5631313675d9b12efb11ce66c8e5818"
console.log('Authorization: ' + authHeader)

