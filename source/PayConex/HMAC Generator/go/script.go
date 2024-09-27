package main

import (
  "strings"
	"crypto/hmac"
  // "crypto/rand"
	"crypto/sha256"
	"fmt"
	"os"
	"time"

	"github.com/google/uuid"
)

func generate(method string, path string, apiKeyId string, apiKeySecret string, body []byte) (string, error) {
	timestamp := time.Now().Unix()

	payloadHash := sha256.Sum256(body)

	u1 := strings.Join(strings.Split(uuid.New().String(), "-"), "")

	u2 := strings.Join(strings.Split(uuid.New().String(), "-"), "")

	nonce := u1 + u2

	canonicalRequest := fmt.Sprintf("%s %s\n%s\n%d\n\n%x",
		method, path, nonce, timestamp, payloadHash)

	h := hmac.New(sha256.New, []byte(apiKeySecret))
	h.Write([]byte(canonicalRequest))
	digest := h.Sum(nil)

	fmt.Println("timestamp: ", timestamp)
	fmt.Println("nonce: ", nonce)
	fmt.Println("payload_hash: ", payloadHash)
	fmt.Println("body: ", body)
	fmt.Println("canonical_request: ", canonicalRequest)

	return fmt.Sprintf("Authorization: Hmac id=\"%s\", nonce=\"%s\", timestamp=\"%d\", response=\"%x\"",
		apiKeyId, nonce, timestamp, digest), nil
}

func main() {
	method := "GET"
	uri := os.Getenv("URI")
	apiKeyId := os.Getenv("API_KEY_ID")
	apiKeySecret := os.Getenv("API_KEY_SECRET")
	body := []byte("")

	hmacHeader, err := generate(method, uri, apiKeyId, apiKeySecret, body)
	if err != nil {
		fmt.Println("Error: ", err)
	}
	fmt.Println(hmacHeader)
}
