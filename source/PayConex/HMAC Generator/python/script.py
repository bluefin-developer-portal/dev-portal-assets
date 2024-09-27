from time import time

from os import environ

import json
import uuid
import hashlib
import hmac


def generate(method: str, path: str, api_key_id: str, secret: str, body: str):
    timestamp = str(int(time()))
    nonce = uuid.uuid4().hex + uuid.uuid4().hex
    
    if method != 'GET' and method != 'DELETE':
        if isinstance(body, dict):
            body = json.dumps(body) 
    else:
        body = ''

    payload_hash = hashlib.sha256(bytes(body, 'utf-8')).hexdigest()

    canonical_request = ('' + 
        method + ' ' + path + '\n' 
        + nonce + '\n' 
        + timestamp + '\n\n' 
        + payload_hash)

    digest = hmac.new(
            bytes(secret, 'utf-8'),
            msg=bytes(canonical_request, 'utf-8'),
            digestmod=hashlib.sha256
   ).hexdigest()

    print('timestamp: ', timestamp)
    print('nonce: ', nonce)
    print('payload_hash: ', payload_hash)
    print('body: ', body)

    print('canonical_request: ', canonical_request)

    return ("Authorization: Hmac " 
        + "id=\"" + api_key_id + "\""
        + ", nonce=\"" + nonce + "\""
        + ", timestamp=\"" + timestamp + "\""
        + ", response=\"" + digest + "\"")


if __name__ == '__main__':
    METHOD = 'GET'
    URI = environ.get('URI')
    API_KEY_ID = environ.get('API_KEY_ID')
    API_KEY_SECRET = environ.get('API_KEY_SECRET')
    BODY = ''

    # print(URI, API_KEY_ID, API_KEY_SECRET)

    hmac_header = generate(METHOD, URI, API_KEY_ID, API_KEY_SECRET, BODY)

    print(hmac_header)
