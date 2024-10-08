curl --location 'https://proxy-staging-tul.shieldconex.com/api/v1/partners/{partnerID}/configurations/{configurationID}' \
--header 'custom-header-test: yes' \
--header 'Authorization: {AUTH_KEY}' \
--header 'scx-bfid: {BLUEFIN_ID}' \
--header 'Content-Type: application/json' \
--data '{
    "User" : {
        "Email" : "{TOKENIZED_VALUE}",
        "Password" : "{TOKENIZED_VALUE}",
        "Address" : " {TOKENIZED_VALUE}",
        "City": "{TOKENIZED_VALUE}",
        "State": "{TOKENIZED_VALUE}",
        "ZIPCode": "{TOKENIZED_VALUE}",
        "Country": "{TOKENIZED_VALUE}"
    }
}'

