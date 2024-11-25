curl -X POST --url "127.0.0.1:3000/api/config" \
	--data '
{
  "label": "Multi-Payment iframe",
  "language": "ENGLISH",
  "timeout": 600,
  "allowedPaymentMethods": [
    "CARD",
    "ACH",
    "GOOGLE_PAY",
    "CLICK_TO_PAY"
  ],
  "allowedParentDomains": [
    "*"
  ],
  "cardSettings": {
    "cvv": "required",
    "billingAddress": {
      "address1": "required",
      "address2": "optional",
      "city": "required",
      "state": "required",
      "zip": "required"
    },
    "capturePhone": "omit",
    "threeDSecure": "required",
    "captureEmail": "omit",
    "captureShippingAddress": false
  },
  "achSettings": {
    "billingAddress": {
      "address1": "required",
      "address2": "optional",
      "city": "required",
      "state": "required",
      "zip": "required"
    },
    "capturePhone": "omit",
    "captureEmail": "omit",
    "captureShippingAddress": false
  },
  "clickToPaySettings": {
    "srcDpaId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    "dpaName": "PayConex",
    "dpaPresentationName": "PayConex App",
    "allowedCardBrands": [
      "VISA",
      "MASTERCARD",
      "AMERICAN_EXPRESS",
      "DISCOVER",
      "CHINA_UNION_PAY"
    ]
  },
  "googlePaySettings": {
    "merchantId": "12345678901234567890",
    "merchantName": "Demo Merchant",
    "billingAddressRequired": true,
    "shippingAddressRequired": true,
    "emailRequired": true,
    "billingAddressParameters": {
      "format": "MIN",
      "phoneNumberRequired": true
    },
    "shippingAddressParameters": {
      "allowedCountryCodes": ["US"],
      "phoneNumberRequired": true
    },
    "allowedAuthMethods": [
      "PAN_ONLY",
      "CRYPTOGRAM_3DS"
    ],
    "allowedCardBrands": [
      "VISA",
      "MASTERCARD",
      "AMERICAN_EXPRESS",
      "DISCOVER",
      "JCB",
      "INTERAC"
    ]
  },
  "currency": "USD",
  "savePaymentOption": "required"

}'
