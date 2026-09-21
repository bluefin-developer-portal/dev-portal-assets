# Apple Pay + PayConex Example

A small demo app that shows how to:

1. Register a website domain for Apple Pay with PayConex
2. Take an Apple Pay payment and charge it through PayConex QSAPI

You run it on your computer with Docker. No Node.js or build step is required.

---

## Run it locally (start here)

Follow these steps in order. When you finish, you will open the app at:

**https://applepay.local:8443/**

### Before you start

You need:

- A Mac (Safari + Apple Pay work best for this demo)
- [Docker Desktop](https://www.docker.com/products/docker-desktop/) installed and running
- PayConex **certification** credentials:
  - QSAPI account id + access key
  - v4 API key id + secret
- An [Apple Pay sandbox](https://developer.apple.com/apple-pay/sandbox-testing/) tester account / card for authorizing payments

### Step 1 — Copy the project and add your credentials

In Terminal, go into this project folder, then run:

```bash
cp config.example.php config.php
```

Open `config.php` in a text editor and replace these placeholders with your real cert credentials:

| Find this in `config.php` | Put your… |
|---------------------------|-----------|
| `YOUR_ACCOUNT_ID` | QSAPI account id |
| `YOUR_QSAPI_ACCESS_KEY` | QSAPI access key |
| `YOUR_API_KEY_ID` | v4 API key id |
| `YOUR_API_KEY_SECRET` | v4 API key secret |

Leave the other settings as they are for local testing.

Do **not** commit `config.php` — it contains secrets.

### Step 2 — Point `applepay.local` at your computer

Apple Pay needs a real hostname (not plain `localhost`). This project uses `applepay.local`.

1. Open Terminal
2. Run:

```bash
sudo nano /etc/hosts
```

3. Enter your Mac password if asked
4. Add this line at the bottom:

```text
127.0.0.1   applepay.local
```

5. Save and exit (`Ctrl+O`, Enter, then `Ctrl+X` in nano)

### Step 3 — Start the app

From the project folder, run:

```bash
docker compose up --build
```

Leave this Terminal window open. The first run can take a minute while Docker builds the image.

You will know it worked when the Terminal stops with logs and does not immediately exit with an error.

### Step 4 — Trust the local HTTPS certificate (one time)

The app uses HTTPS with a local certificate. Safari must trust it once.

With Docker still running, open a **second** Terminal window in the project folder and run:

```bash
docker compose cp https:/data/caddy/pki/authorities/local/root.crt ./caddy-root.crt
sudo security add-trusted-cert -d -r trustRoot -k /Library/Keychains/System.keychain ./caddy-root.crt
```

Then:

1. Quit Safari completely (Safari → Quit Safari) and reopen it
2. You can delete `caddy-root.crt` afterward if you want

### Step 5 — Open the app

In Safari, go to:

**https://applepay.local:8443/**

You should see the demo home page with **Registration** and **Checkout**.

If Safari warns about the certificate, go back to Step 4.

> Tip: Do **not** use `http://localhost:8080` for Apple Pay. That address is only for quick API checks. Apple Pay needs the HTTPS `applepay.local` URL.

### Step 6 — Register your local domain

1. Open **https://applepay.local:8443/register.html**
2. Confirm **Current enrollment** loads (lists domains already registered on your account)
3. Under **Domains to register**, keep `applepay.local` (or add it)
4. Confirm **Merchant URL** is `applepay.local` (hostname only — no `https://`, no `:8443`)
5. Click **Register merchant**
6. Check that the response shows success

Optional check: open  
**https://applepay.local:8443/.well-known/apple-developer-merchantid-domain-association**  
in Safari. You should see a long text file, not an error page.

### Step 7 — Make a test payment

1. Open **https://applepay.local:8443/checkout.html** in Safari
2. Wait until the status says Apple Pay is ready
3. Enter an amount (default `1.00` is fine)
4. Click the Apple Pay button and authorize with Touch ID / your sandbox wallet
5. Watch the **Payment flow** panel:
   - Merchant session
   - Apple Pay token
   - QSAPI SALE

Approved or declined both count as a successful demo run. Sandbox cards can decline on cert processors — that can be normal.

### Stop the app

In the Terminal where Docker is running, press `Ctrl+C`, or run:

```bash
docker compose down
```

---

## If something goes wrong

| Problem | What to try |
|---------|-------------|
| Safari certificate warning | Redo Step 4, quit Safari fully, try again |
| “Apple Pay is not available” / insecure page | Use `https://applepay.local:8443`, not `http://localhost:8080` |
| Registration fails | Double-check `config.php` credentials; confirm Docker is running |
| Payment sheet closes with no token | Confirm you registered `applepay.local` (exact hostname). Domain in the address bar must match enrollment |
| Touch ID / sheet cancels early | Use a Mac user with Touch ID enrolled and a sandbox card in Wallet |
| `docker compose` errors | Make sure Docker Desktop is running, then retry from the project folder |

---

## What the pages do

| Page | Purpose |
|------|---------|
| Home | Overview and links |
| Registration | View enrolled domains; register new ones (HMAC signed on the server) |
| Checkout | Apple Pay button → PayConex session → QSAPI SALE |

Secrets stay in `config.php` on the server. The browser never sees your API secret.

Official docs this example follows:

- [Apple Pay Intro](https://developers.bluefin.com/payconex/docs/apay-intro)
- [Environment Setup](https://developers.bluefin.com/payconex/docs/getting-started-1)
- [Button Integration](https://developers.bluefin.com/payconex/docs/integrating-apple-pay)
- [Using Apple Pay Tokens](https://developers.bluefin.com/payconex/docs/using-apple-pay-token)

---

## Deploy to a web host (cPanel)

Use this when you want the demo on a real HTTPS hostname (for example a shared tools server) instead of Docker.

Upload at least:

```text
config.php          ← your credentials (create from config.example.php)
src/
public/             ← point the site document root here
```

Then:

1. Confirm the site uses HTTPS
2. Confirm  
   `https://YOUR-DOMAIN/.well-known/apple-developer-merchantid-domain-association`  
   loads
3. Open `/register.html`, register `YOUR-DOMAIN` (hostname only)
4. Test `/checkout.html` in Safari

This repo ships the **certification** association file. For production, replace it with Bluefin’s prod file (remove the `.prod` suffix when saving):

https://raw.githubusercontent.com/bluefin-developer-portal/dev-portal-assets/main/payconex/apple-pay-certificates/prod/apple-developer-merchantid-domain-association.prod

Also switch `config.php` to production PayConex URLs and keys.

---

## Project files (quick map)

| Path | What it is |
|------|------------|
| `config.example.php` | Credential template — copy to `config.php` |
| `config.php` | Your secrets (gitignored) |
| `public/` | Web pages, JS, CSS, API endpoints, Apple domain file |
| `src/` | PHP helpers (HMAC, registration, QSAPI) |
| `docker-compose.yml` | Start the local stack from the repo root |
| `docker/Dockerfile` | PHP 8.3 Apache image |
| `docker/Caddyfile` | Local HTTPS for `applepay.local` |

---



## Implementation notes

### Merchant registration HMAC

Handled only in PHP (`PayConexHmac`):

1. `ContentHash = hex(SHA-256(raw JSON body))`
2. `string-to-hash = METHOD + " " + path + "\n" + nonce + "\n" + timestamp + "\n\n" + ContentHash`
3. `response = hex(HMAC-SHA256(api_key_secret, string-to-hash))`
4. Header: `Authorization: Hmac id="…", nonce="…", timestamp="…", response="…"`

### QSAPI Apple Pay SALE

- `tender_type=APay`
- `transaction_type=SALE`
- `apay_payload` = Apple Pay token JSON (includes `paymentData`)
- `response_format=JSON`

### Apple Pay session

Checkout follows the Bluefin button guide: the browser posts to

`POST {v4.api_url}/api/v4/accounts/{account_id}/applePay/session`

with body `{ display_name, referrer }`, then calls `completeMerchantValidation()` with the JSON response.



### Endpoints Used

Use the following endpoints in the order shown when integrating and registering Apple Pay with your PayConex merchant account. The example app follows the same sequence.

> 📘 Note
>
> All of the API endpoints should be prefixed by the PayConex environment URL you are currently integrating with. See [API Endpoint Overview | Environment URLs](https://developers.bluefin.com/payconex/v4/reference/api-endpoint-overview#environment-urls).

| Endpoint                                              | Method   | Description                                                  |
| ----------------------------------------------------- | -------- | :----------------------------------------------------------- |
| `/api/v1/apple_pay/merchant_registration/{accountId}` | **POST** | Use this endpoint in order to enroll an account with Apple Pay.<br />Any developer wanting to display the Apple Pay button on their pages must register a merchant's PayConex account to use Apple Pay. To facilitate this requirement an Apple Pay merchant registration API end-point is available via the PayConex API.<br />For more, see [Apple Pay Intro](https://developers.bluefin.com/payconex/docs/apay-intro) and [Environment Setup](https://developers.bluefin.com/payconex/docs/getting-started-1). |
| `/api/v4/accounts/{accountId}/applePay/enrollment`    | **GET**  | Check enrollment/registration details.<br />Use this endpoint to confirm that account enrollment succeeded and that the requested domains are configured. |
| `/api/v4/accounts/${accountId}/applePay/session`      | **POST** | Get an Apple Pay session for the merchant to use.<br />This step is required to complete the merchant validation and proceed to authorizing the payment.<br />On Apple Pay SDK `session.onpaymentauthorized`, the Apple Pay token is received for use in a payment.<br />For more, see [Button Integration](https://developers.bluefin.com/payconex/docs/integrating-apple-pay). |
| `/api/qsapi/3.8`                                      | **POST** | Using the token received by the Apple Pay SDK, we are set to process a `SALE` transaction using the [PayConex APIs](https://developers.bluefin.com/payconex/docs/process-transactions-ach-updates).<br />For environment URLs for QSAPI, refer to [Process Transactions API URLs](https://developers.bluefin.com/payconex/docs/process-transactions-ach-updates#api-urls).<br />For more on this step, see [Using Apple Pay Tokens](https://developers.bluefin.com/payconex/docs/using-apple-pay-token). |





