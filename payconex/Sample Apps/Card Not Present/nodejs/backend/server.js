import 'dotenv/config'
import express from 'express'
import fetch from 'node-fetch'
import cors from 'cors'
import sqlite3 from 'sqlite3'
import { open } from 'sqlite'
import { v4 as uuidv4 } from 'uuid'


const PORT = process.env.PORT || 3000
// PayConex account ID number
const ACCOUNT_ID = process.env.ACCOUNT_ID
// Base64 encoding of PayConex API Key id:secret
const BASIC_TOKEN = process.env.BASIC_TOKEN
const ENVIRONMENT_URL = 'https://api-cert.payconex.net'
const IFRAME_CONFIG_ID = process.env.IFRAME_CONFIG_ID

let db

const app = express()
app.use(express.json())
app.use(cors())

if(process.env.sample_app_debug == 1) {
  console.log('ACCOUNT_ID: ', ACCOUNT_ID)
  console.log('BASIC_TOKEN: ', BASIC_TOKEN)
  console.log('IFRAME_CONFIG_ID: ', IFRAME_CONFIG_ID)
}

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
        token: subscription.token,
        amount: subscription.amount,
        typeSubscription: subscription.type_subscription
      })
    })

    return res.json({ subscriptionList })
  } catch (err) {
    console.log('Error getting the subscriptions: ', err)
  }
})

// See: https://developers.bluefin.com/payconex/v4/reference/processing-a-sale
// And: https://developers.bluefin.com/payconex/v4/reference/customer-and-merchant-initiated-transactions-1
async function processSale(req, res) {
  const { subscription } = req.body

  const subsequentTransactionToken = subscription?.token
  const amount = subscription?.amount

  if (!subsequentTransactionToken || !amount) {
    return res.status(400).json({ 'message': 'Missing subscription data for process sale.' })
  }

  // NOTE: Or use the shieldconex tokens for the recurring payments as MITs. See https://developers.bluefin.com/payconex/v4/reference/customer-and-merchant-initiated-transactions-1. But this requires storing customer information separately in your database as the shieldconex token only consists of the tokenized card data. 
  const subsequentSaleBody = {
    "bfTokenReference": subsequentTransactionToken,
    "posProfile": "SERVER",
    "amounts": {
      "total": amount,
      "currency": "USD"
    },
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
    })

    const subsequentProcessSaleData = await subsequentProcessSaleResponse.json()
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
  
  
  const instance_merchant_reference = uuidv4()
  
  const iframeInstanceReqBody = {
    "label": "Card only iframe instance",
    "language": "ENGLISH",
    "amount": amount,
    "reference": instance_merchant_reference,
    "timeout": 800,
    "allowedPaymentMethods": [
      "CARD",
      "ACH"
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
    "currency": "USD",
    "threeDSecureInitSettings": {
      "transactionType": "GOODS_SERVICE_PURCHASE",
      "deliveryTimeFrame": "ELECTRONIC_DELIVERY",
      "threeDSecureChallengeIndicator": "NO_PREFERENCE",
      "reorderIndicator": "FIRST_TIME_ORDERED",
      "shippingIndicator": "BILLING_ADDRESS"
    }
  }
  
  if(process.env.sample_app_debug == 1) {
    console.log('iframeInstanceReqBody', iframeInstanceReqBody)
  }

  try {
  
    // Creating Iframe configuration. However, we use one global configuration and integrate with the instance creation.
    /*
    const iframeResponse = await fetch(`${ENVIRONMENT_URL}/api/v4/accounts/${ACCOUNT_ID}/payment-iframe`, {
      method: 'post',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Basic ${BASIC_TOKEN}`
      },
      body: JSON.stringify(iframeReqBody)
    })

    const createdComponent = await iframeResponse.json()
    const resourceId = createdComponent.id

    const instanceReqBody = {
      "label": "my-instance-2",
      "amount": amount,

    }
    */

    // NOTE: We recommend having one global iframe configuration and overwriting that Iframe configuration based on a customer. Of course, you can also have multiple iframe configurations but we don't recommend creating one each time. See https://developers.bluefin.com/payconex/v4/reference/creating-an-instance#overwriting-configuration
    const instanceResponse = await fetch(`${ENVIRONMENT_URL}/api/v4/accounts/${ACCOUNT_ID}/payment-iframe/${IFRAME_CONFIG_ID}/instance/init`, {
      method: 'post',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Basic ${BASIC_TOKEN}`
      },
      body: JSON.stringify(iframeInstanceReqBody)
    })

    const createdInstance = await instanceResponse.json()
    
    if(instanceResponse.status >= 400) {
      res.status(instanceResponse.status)
      return res.json(createdInstance)
    }
    
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
    "bfTokenReference": payConexToken,
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

    const storedCardData = await storeCardResponse.json()
    
    if(storeCardResponse.status >= 400) {
      res.status(storeCardResponse.status)
      return res.json(storedCardData)
    }
    
    const subsequentTransactionToken = storedCardData.bfTokenReference
    const { shieldConexToken } = storedCardData

    const tokenizedCardData = {
      bfid: shieldConexToken.bfid,
      token: subsequentTransactionToken,
      cardNumber: shieldConexToken.tokenCardNumber,
      cardExpiration: shieldConexToken.tokenCardExpiration,
      typeSubscription,
      amount
    }
    
    if(process.env.sample_app_debug == 1) {
      console.log('tokenizedCardData', tokenizedCardData)
    }

    await db.run(
      'INSERT INTO subscriptions(bfid, token, card_number, card_expiration, type_subscription, amount)'
      + ' VALUES (?, ?, ?, ?, ?, ?)',
      [
        tokenizedCardData.bfid,
        tokenizedCardData.token,
        tokenizedCardData['cardNumber'],
        tokenizedCardData['cardExpiration'],
        tokenizedCardData.typeSubscription,
        tokenizedCardData.amount
      ]
    )

    return res.status(201).json({ "token": tokenizedCardData.token })
  } catch (err) {
    console.log('Error creating the subscription: ', err)
  }
})

// ISV implements the recurring payment solution (the interval) to trigger this endpoint and, thus, the recurring payment. PayConex V4 API V4 does not yet support the scheduled recurring payments feature. However, This feature can be set via the PayConex v3.8 API SLAPI or from the PayConex® Portal via UI tools -> RECURRING BILLING.
app.post('/recurring-payment', async function(req, res) {
  const { token } = req.body

  if (!token) {
    return res.status(400).json({ 'message': 'Missing Bluefin token for recurring payment.' })
  }

  try {
    const subscription = await db.get('SELECT * FROM subscriptions WHERE token = ?', token)
    console.log('Recurring payment subscription data: ', subscription)

    if (!subscription) {
      return res.status(404).json({ 'message': 'Subscription data not found for recurring payment' })
    }

    req.body.subscription = subscription
    return processSale(req, res)
  } catch (err) {
    console.log('Error processing the recurring payment: ', err)
  }
})

// Deletes a subscription using the Bluefin ID.
app.delete('/subscription/:token', async function(req, res) {
  const token = req.params.token

  if (!token) {
    return res.status(400).json({ 'message': 'Missing Bluefin token for cancelling subscription.' })
  }

  try {
    await db.run('DELETE FROM subscriptions WHERE token = ?',
      [token])

    return res.status(204).json()
  } catch (err) {
    console.log('Error deleting the subscription: ', err)
  }
})

;(async () => {
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
            type_subscription TEXT NOT NULL,
            amount TEXT NOT NULL
        )
    `)
  } catch (err) {
    console.log('Error opening the DB and creating the Subscriptions table: ', err)
  }

  app.listen(PORT, () => console.log(`Server running on port ${PORT}`))
})()
