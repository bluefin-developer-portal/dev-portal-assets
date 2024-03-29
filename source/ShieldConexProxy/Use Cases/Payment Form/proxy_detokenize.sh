curl --location 'https://proxy-staging-tul.shieldconex.com/api/v1/partners/{partnerID}/configurations/{configurationID}' \
--header 'custom-header-test: yes' \
--header 'Authorization: {AUTH_KEY}' \
--header 'scx-bfid: {BLUEFIN_ID}' \
--header 'Content-Type: application/json' \
--data '{
    "Card" : {
        "CCN" : "{TOKENIZED_VALUE}",
        "Expiry": "{TOKENIZED_VALUE}",
        "CVV": "{TOKENIZED_VALUE}",
        "BAN": "{TOKENIZED_VALUE}",
        "RTN": "{TOKENIZED_VALUE}"
    }
}'

