package main

import (
	"bytes"
	"encoding/json"
	"fmt"
	"io"
	"log"
	"net/http"
	"os"
)

func (app *App) handlerGenerateConfig(w http.ResponseWriter, r *http.Request) {
	type IframeConfig struct {
		ResourceId string `json:"id"`
	}

	accountId, basicToken, environmentUrl := os.Getenv("ACCOUNT_ID"),
		os.Getenv("BASIC_TOKEN"), "https://api-cert.payconex.net"

	reqBody, err := io.ReadAll(r.Body)
	if err != nil {
		log.Printf("Error reading generateConfig req body: %v", err)
		http.Error(w, "Something wrong. Please try again later.", http.StatusInternalServerError)
		return
	}

	iframeReq, err := http.NewRequest("POST", fmt.Sprintf("%v/api/v4/accounts/%v/payment-iframe",
		environmentUrl, accountId),
		bytes.NewBuffer(reqBody))
	if err != nil {
		log.Printf("Error setting a request to payment-iframe: %v", err)
		http.Error(w, "Something wrong. Please try again later.", http.StatusInternalServerError)
		return
	}

	iframeReq.Header.Set("Content-Type", "application/json")
	iframeReq.Header.Set("Authorization", fmt.Sprintf("Basic %v", basicToken))

	client := &http.Client{}
	iframeResp, err := client.Do(iframeReq)
	if err != nil {
		log.Printf("Error making a request to payment-iframe: %v", err)
		http.Error(w, "Something wrong. Please try again later.", http.StatusInternalServerError)
		return
	}
	defer iframeResp.Body.Close()

	iframeRespBody, err := io.ReadAll(iframeResp.Body)
	if err != nil {
		log.Printf("Error reading payment-iframe response: %v", err)
		http.Error(w, "Something wrong. Please try again later.", http.StatusInternalServerError)
		return
	}

	var iframeConfig IframeConfig
	if err = json.Unmarshal(iframeRespBody, &iframeConfig); err != nil {
		log.Printf("Error parsing payment-iframe response body: %v", err)
		http.Error(w, "Something wrong. Please try again later.", http.StatusInternalServerError)
		return
	}

	if _, err = app.DB.Exec("INSERT INTO checkout_config(resource_id, config) VALUES ( ?, ? )",
		iframeConfig.ResourceId, iframeRespBody); err != nil {
		log.Printf("Error executing INSERT query into checkout config: %v", err)
		http.Error(w, "Something wrong. Please try again later.", http.StatusInternalServerError)
		return
	}

	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(iframeResp.StatusCode)
	if _, err = w.Write(iframeRespBody); err != nil {
		log.Printf("Error writing checkout config body to connection: %v", err)
		http.Error(w, "Something wrong. Please try again later.", http.StatusInternalServerError)
		return
	}
}
