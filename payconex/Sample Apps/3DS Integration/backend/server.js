const express = require('express')


const PORT = process.env.PORT || 3000
// PayConex account ID number
const ACCOUNT_ID = process.env.ACCOUNT_ID
// See: https://developers.bluefin.com/payconex/docs/server-side-3ds-integration#generating-bearer-token
const BEARER_TOKEN = process.env.PAYCONEX_BEARER_TOKEN
const URL = 'https://api-cert.payconex.net'

const app = express()
app.use(express.json())

if(process.env.SAMPLE_APP_DEBUG == 1) {
  console.log('ACCOUNT_ID: ', ACCOUNT_ID)
  console.log('BEARER_TOKEN: ', BEARER_TOKEN)
}

app.post('/init-card', async function(req, res) {
  let body = req.body
  let card_res = await fetch(`${URL}/api/v4/accounts/${ACCOUNT_ID}/3DS/init-card-details`, {
    method: "POST",
    headers: {
      "content-type": "application/json",
      "Authorization": `Bearer ${BEARER_TOKEN}`,
    },
    body: JSON.stringify(body),
  })

  res.status(card_res.status)
  return res.json(await card_res.json())
})

app.post('/request-acs-form', async function(req, res) {
  try {
    let body = req.body

    let html_form = `<!DOCTYPE html>
        <html lang='en'>
        <head>
            <title>Redirect user's browser to ACS</title>
        </head>
        <body>
            <form id="submitForm" action="${body.acsUrl ?? ''}" method="POST">
               <input name="threeDSMethodData" value="${body.threeDSecureData ?? ''}"/>
                <script>
                    const a = 1;
                </script>
                <noscript>
                    JavaScript is not supported or is disabled. Please hit the Submit but
                    ton.
                   <input type="submit" value="Submit"/>
                </noscript>
            </form>
            <script>
                window.onload = function () {
                    document.getElementById("submitForm").submit();
                }
            </script>
        </body>
        </html>`

    res.status(200)
    return res.json({
      html: html_form,
    })


  } catch(err) {
    res.status(400)
    return res.json({ error: err.toString() })
  }
})

app.post('/browser-authenticate', async function(req, res) {
  let body = req.body
  let threeDSecureId = body.threeDSecureId
  delete body.threeDSecureId
  let card_res = await fetch(`${URL}/api/v4/accounts/${ACCOUNT_ID}/3DS/${threeDSecureId}/browser-authenticate`, {
    method: "POST",
    headers: {
      "content-type": "application/json",
      "Authorization": `Bearer ${BEARER_TOKEN}`,
    },
    body: JSON.stringify(body),
  })

  res.status(card_res.status)
  return res.json(await card_res.json())
})

app.post('/3ds-status', async function(req, res) {
  let body = req.body
  let threeDSecureId = body.threeDSecureId
  const status_URL = `${URL}/api/v4/accounts/${ACCOUNT_ID}/3DS/${threeDSecureId}/status`
  let status_res = await fetch(status_URL, {
    method: "GET",
    headers: {
      Authorization: `Bearer ${BEARER_TOKEN}`,
    }
  })
  let threeDSdata = await status_res.json()

  res.status(status_res.status)

  return res.json(threeDSdata)
})


;(async function main() {

  // Start the server
  app.listen(PORT, () => console.log(`Server running on port ${PORT}`))
})();
