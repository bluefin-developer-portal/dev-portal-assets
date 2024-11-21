require 'json'
require 'net/http'
require 'digest'
require 'securerandom'

def generate(method, path, api_key_id, secret, body)
  timestamp = Time.now.to_i.to_s
  nonce = SecureRandom.hex(32)

  body = (method != 'GET' && method != 'DELETE') ? JSON.generate(ent.data) : ''

  payload_hash = Digest::SHA256.hexdigest(body)

  canonical_request = [
    "#{method} #{path}",
    nonce,
    timestamp,
    '',
    payload_hash
  ].join("\n")

  digest = OpenSSL::HMAC.hexdigest('SHA256', secret, canonical_request)

  puts('timestamp: ' + timestamp)
  puts('nonce: ' + nonce)
  puts('payload_hash: ' + payload_hash)
  puts('canonical_request: ' + canonical_request)

  puts('body: ', body)

  return "Authorization: Hmac " + "id=\"#{api_key_id}\"" + ", nonce=\"#{nonce}\"" + ", timestamp=\"#{timestamp}\"" + ", response=\"#{digest}\""

end


# puts ENV.inspect

METHOD = "GET"
URI_PATH = ENV["URI"]
API_KEY_ID = ENV["API_KEY_ID"]
API_SECRET_KEY = ENV["API_SECRET_KEY"]
BODY = "" # { "bfTokenReference" => "...", ... }

hmac_header = generate(METHOD, URI_PATH, API_KEY_ID, API_SECRET_KEY, BODY)

puts hmac_header
