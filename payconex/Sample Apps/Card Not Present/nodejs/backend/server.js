import 'dotenv/config'
import express from 'express'
import fetch from 'node-fetch'
import cors from 'cors'
import sqlite3 from 'sqlite3'
import { open } from 'sqlite'
import { jwtDecode } from 'jwt-decode'

const PORT = process.env.PORT || 3000
// PayConex account ID number
const ACCOUNT_ID = process.env.ACCOUNT_ID
// Base64 encoding of PayConex API Key id:secret
const BASIC_TOKEN = process.env.BASIC_TOKEN
const ENVIRONMENT_URL = 'https://api-cert.payconex.net'
const TEMPLATE_REFERENCE = process.env.TEMPLATE_REFERENCE
let db

const app = express()
app.use(express.json())
app.use(cors())

// List all subscriptions that belong to that user
app.get('/subscription', async function(req, res) {
  try {
    // SELECT ALL query implemented for the sake of the example.
    // If implementing a similar endpoint, use an appropriate
    // query for your context and pagination.
    const subscriptions = await db.all('SELECT * FROM subscriptions')
    console.log('subscriptions: ', subscriptions)

    if (!subscriptions) {
      return res.status(404).json({ 'message': 'No subscription found.' })
    }

    const subscriptionList = []

    subscriptions.forEach(subscription => {
      subscriptionList.push({
        bluefinId: subscription.bfid,
        amount: subscription.amount,
        typeSubscription: subscription.type_subscription
      })
    })

    return res.json({ subscriptionList })
  } catch (err) {
    console.log('Error getting the subscriptions: ', err)
  }
});

// See: https://developers.bluefin.com/payconex/v4/reference/processing-a-sale
// And: https://developers.bluefin.com/payconex/v4/reference/customer-and-merchant-initiated-transactions-1
async function processSale(req, res) {
  const { subscription } = req.body

  const subsequentTransactionToken = subscription?.token
  const amount = subscription?.amount

  if (!subsequentTransactionToken || !amount) {
    return res.status(400).json({ 'message': 'Missing subscription data for process sale.' })
  }

  const subsequentSaleBody = {
    "token": subsequentTransactionToken,
    "posProfile": "MOTO",
    "amounts": { "total": amount, "currency": "USD" },
    "credentialOnFileOverride": {
      "transactionInitiator": "MERCHANT",
      "storedCredentialIndicator": "SUBSEQUENT",
      "cardOnFileScheduleIndicator": "SCHEDULED"
    }
  }

  try {
    const subsequentProcessSaleResponse = await fetch(`${ENVIRONMENT_URL}/api/v4/accounts/${ACCOUNT_ID}/payments/sale`, {
      method: 'post',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Basic ${BASIC_TOKEN}`
      },
      body: JSON.stringify(subsequentSaleBody)
    });

    const subsequentProcessSaleData = await subsequentProcessSaleResponse.json();
    return res.json({ 'transactionData': subsequentProcessSaleData })
  } catch (err) {
    console.log('Error processing the sale: ', err)
  }
}

// Creates a checkout configuration component and its instance
app.post('/generate-bearer-token', async function generateBearerToken(req, res) {
  const { amount } = req.body

  if (!amount) {
    return res.status(400).json({ 'message': 'Missing amount to generate bearer token.' })
  }

  const iframeReqBody = {
    "label": "Card only iframe",
    "language": "ENGLISH",
    "timeout": 800,
    "allowedPaymentMethods": [
      "CARD",
      "ACH"
    ],
    "allowedParentDomains": [
      "*"
    ],
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
    "savePaymentOption": "required",
    "currency": "USD"
  }

  try {
    const iframeResponse = await fetch(`${ENVIRONMENT_URL}/api/v4/accounts/${ACCOUNT_ID}/payment-iframe`, {
      method: 'post',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Basic ${BASIC_TOKEN}`
      },
      body: JSON.stringify(iframeReqBody)
    })

    const createdComponent = await iframeResponse.json();
    const resourceId = createdComponent.id

    const instanceReqBody = {
      "label": "my-instance-2",
      "amount": amount,
      "reference": TEMPLATE_REFERENCE,
      "threeDSecureInitSettings": {
        "transactionType": "GOODS_SERVICE_PURCHASE",
        "deliveryTimeFrame": "ELECTRONIC_DELIVERY",
        "threeDSecureChallengeIndicator": "NO_PREFERENCE",
        "reorderIndicator": "FIRST_TIME_ORDERED",
        "shippingIndicator": "BILLING_ADDRESS"
      }
    }

    const instanceResponse = await fetch(`${ENVIRONMENT_URL}/api/v4/accounts/${ACCOUNT_ID}/payment-iframe/${resourceId}/instance/init`, {
      method: 'post',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Basic ${BASIC_TOKEN}`
      },
      body: JSON.stringify(instanceReqBody)
    })

    const createdInstance = await instanceResponse.json();
    return res.json({ 'bearerToken': createdInstance.bearerToken })
  } catch (err) {
    console.log('Error generating the bearer token: ', err)
  }
})

// Create a subscription with the tokenized card data.
// Please see: https://developers.bluefin.com/payconex/v4/reference/storing-a-card-on-file
app.post('/subscription', async function(req, res) {
  const { payConexToken, typeSubscription, amount } = req.body

  if (!payConexToken || !amount || !typeSubscription) {
    return res.status(400).json({ 'message': 'Missing subscription data for creation.' })
  }

  const body = {
    "token": payConexToken,
    "posProfile": "ECOMMERCE"
  }

  try {
    const storeCardResponse = await fetch(`${ENVIRONMENT_URL}/api/v4/accounts/${ACCOUNT_ID}/payments/store`, {
      method: 'post',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Basic ${BASIC_TOKEN}`
      },
      body: JSON.stringify(body)
    })

    const storedCardData = await storeCardResponse.json();
    const subsequentTransactionToken = storedCardData.token
    const tokenDataDecoded = jwtDecode(subsequentTransactionToken).payload.token
    const tokenizedCardValues = tokenDataDecoded.values

    const tokenizedCardData = {
      bfid: tokenDataDecoded.bfid,
      token: subsequentTransactionToken,
      typeSubscription,
      amount
    }

    const keysToStore = {
      'scx_token_card_number': 'cardNumber',
      'scx_token_card_expiration': 'cardExpiration',
      'scx_token_name': 'tokenName'
    }

    for (const tokenizedCardValue of tokenizedCardValues) {
      if (tokenizedCardValue.name in keysToStore) {
        tokenizedCardData[keysToStore[tokenizedCardValue.name]] = tokenizedCardValue.value
      }
    }

    await db.run(
      'INSERT INTO subscriptions(bfid, token, card_number, card_expiration, token_name, type_subscription, amount)'
      + ' VALUES (?, ?, ?, ?, ?, ?, ?)',
      [
        tokenizedCardData.bfid,
        tokenizedCardData.token,
        tokenizedCardData['cardNumber'],
        tokenizedCardData['cardExpiration'],
        tokenizedCardData['tokenName'],
        tokenizedCardData.typeSubscription,
        tokenizedCardData.amount
      ]
    )

    return res.status(201).json({ "bluefinId": tokenizedCardData.bfid })
  } catch (err) {
    console.log('Error creating the subscription: ', err)
  }
});

// PayConex API V4 will soon support the recurring payments feature.
app.post('/recurring-payment', async function(req, res) {
  const { bfId } = req.body

  if (!bfId) {
    return res.status(400).json({ 'message': 'Missing Bluefin ID for recurring payment.' })
  }

  try {
    const subscription = await db.get('SELECT * FROM subscriptions WHERE bfid = ?', bfId)
    console.log('Recurring payment subscription data: ', subscription)

    if (!subscription) {
      return res.status(404).json({ 'message': 'Subscription data not found for recurring payment' })
    }

    req.body.subscription = subscription
    return processSale(req, res)
  } catch (err) {
    console.log('Error processing the recurring payment: ', err)
  }
});

// Deletes a subscription using the Bluefin ID.
app.delete('/subscription/:bfid', async function(req, res) {
  const bfId = req.params.bfid

  if (!bfId) {
    return res.status(400).json({ 'message': 'Missing Bluefin ID for cancelling subscription.' })
  }

  try {
    await db.run('DELETE FROM subscriptions WHERE bfid = ?',
      [bfId])

    return res.status(204).json()
  } catch (err) {
    console.log('Error deleting the subscription: ', err)
  }
});

(async () => {
  try {
    db = await open({
      filename: './db/subscriptions_database.db',
      driver: sqlite3.Database
    })
    console.log('DB Opened')

    await db.exec(`
        CREATE TABLE IF NOT EXISTS subscriptions (
            id INTEGER PRIMARY KEY,
            token TEXT NOT NULL,
            bfid TEXT NOT NULL,
            card_number TEXT NOT NULL,
            card_expiration TEXT NOT NULL,
            token_name TEXT NOT NULL,
            type_subscription TEXT NOT NULL,
            amount TEXT NOT NULL
        )
    `)
  } catch (err) {
    console.log('Error opening the DB and creating the Subscriptions table: ', err)
  }

  app.listen(PORT, () => console.log(`Server running on port ${PORT}`))
})()
