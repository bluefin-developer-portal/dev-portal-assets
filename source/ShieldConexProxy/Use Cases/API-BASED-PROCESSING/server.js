const fetch = require('node-fetch')

const express = require('express')
const sqlite = require('sqlite3')

const app = express()

const PORT = 8000

const BASIC_AUTH = process.env.BLUEFIN_BASIC_AUTH
const TEMPLATE_REF = process.env.BLUEFIN_TEMPLATE_REF
const PARTNER_ID = process.env.BLUEFIN_PARTNER_ID
const CONFIG_REF = process.env.BLUEFIN_CONFIGURATION_REF

const TOKENIZATION_URL = 'https://secure-cert.shieldconex.com/api/tokenization/tokenize'
const PROXY_DETOKENIZATION_URL = `https://proxy-cert.shieldconex.com/api/v1/partners/${PARTNER_ID}/configurations/${CONFIG_REF}`

// read card sensitive data to tokenize on file
const cards = require('./cards.json')

let db = null

async function tokenizeCardData(cardData) {
  const ccn = cardData.ccn
  const expiry = cardData.expiry
  const cvv = cardData.cvv
  let res = await fetch(TOKENIZATION_URL, { 
    method: 'POST',
    headers: {
      Authorization: 'Basic ' + BASIC_AUTH,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({
      reference: 'myref',
      templateRef: TEMPLATE_REF,
      values: [
        { name: 'card_number', value: ccn },
        { name: 'card_exp', value: expiry },
        { name: 'card_cvv', value: cvv },
      ],
    })

  })

  if(res.status >= 300 || res.status < 200) {
    throw new Error('' + res.status + ': ' + res.statusText)
  }

  return await res.json()

}

async function proxyDetokenize(cardData) {
  const ccn = cardData.ccn
  const expiry = cardData.expiry
  const cvv = cardData.cvv
  const bfid = cardData.bfid

  let res = await fetch(PROXY_DETOKENIZATION_URL, {
    method: 'POST',
    headers: {
      Authorization: 'Basic ' + BASIC_AUTH,
      'scx-bfid': bfid,
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({
      Card: {
        CCN: ccn,
        Expiry: expiry,
        CVV: cvv,
      }
    })

  })

  if(res.status >= 300 || res.status < 200) {
    throw new Error('' + res.status + ': ' + res.statusText)
  }

  return await res.json()

}

// API path to integrate the ShieldConex Proxy Detokenization
// Used to represent the billing cycle where we send the ShieldConex token and BFID to the Proxy for detokenization 
app.post('/processpayments', async function (req, res) {
  if(db == null) {
    res.send('db not setup')
    return
  }

  console.log('calling ShieldConex Proxy API', PROXY_DETOKENIZATION_URL)

  db.all('SELECT * FROM cards', async function(err, cards) {
    let proxy_responses = []

    for(let card of cards) {
      let proxy_res = await proxyDetokenize(card)
      proxy_responses.push(proxy_res)
    }

    console.log(proxy_responses)

    return res.send(JSON.stringify(proxy_responses))
  })

})




;(async function() {

  db = new sqlite.Database('./db/card_database.db', function (err) {
    if(err) {
      console.log('Error: ', err)
      return
    }

    console.log('db opened.')

    // create the cards table to pull tokenized cards from
    db.run(`
        CREATE TABLE IF NOT EXISTS cards (
            id INTEGER PRIMARY KEY,
            ccn TEXT UNIQUE NOT NULL,
            expiry TEXT NOT NULL,
            cvv TEXT NOT NULL,
            bfid TEXT NOT NULL
        )
    `, async function(err, res) {

      if(err == null) {
        console.log('cards table created')

	// prepare to re-insert tokens and BFID into database
        const reinsert = db.prepare('INSERT OR REPLACE INTO cards (ccn, expiry, cvv, bfid) VALUES (?, ?, ?, ?)');

        for(let card of cards) {
          // tokenize sensitive card data
          let res = await tokenizeCardData({ 
            ccn: card.ccn, 
            expiry: card.expiry,
            cvv: card.cvv
          })

          const BFID = res.bfid

          const [ card_number, card_exp, card_cvv ] = res.values

	  // re-insert tokens along with BFID into database
	  // The token and a BFID are returned for storage
          reinsert.run(card_number.value, card_exp.value, card_cvv.value, BFID)

        }
        reinsert.finalize()

      }

    })

  })


  console.log(`LISTENING ON PORT: ${PORT}`)

  app.listen(PORT)

})()
