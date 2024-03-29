curl -X POST --location 'https://secure-staging.shieldconex.com/api/tokenization/read' \
--header 'Authorization: {AUTH_KEY}' \
--header 'Content-Type: application/json' \
--header 'Accept: application/json' \
--data '{
    "bfid": "{BLUEFIN_ID}"
}'

