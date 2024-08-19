package main

import (
	"bytes"
	"encoding/json"
	"fmt"
	"io"
	"log"
	"net/http"
	"os"

	"github.com/go-playground/validator/v10"
)

func (app *App) handlerProcessCapture(w http.ResponseWriter, r *http.Request) {
	type CaptureHandlerRequestBody struct {
		TransactionId string `validate:"required"`
	}

	accountId, basicToken, environmentUrl := os.Getenv("ACCOUNT_ID"),
		os.Getenv("BASIC_TOKEN"),
		"https://api-cert.payconex.net"

	decoder, params := json.NewDecoder(r.Body), CaptureHandlerRequestBody{}
	if err := decoder.Decode(&params); err != nil {
		log.Printf("Error decoding capture parameters: %v", err)
		http.Error(w, "Invalid parameters.", http.StatusBadRequest)
		return
	}

	validate := validator.New(validator.WithRequiredStructEnabled())
	if err := validate.Struct(params); err != nil {
		validationErrors := err.(validator.ValidationErrors)
		log.Printf(validationErrors.Error())
		http.Error(w, "Invalid parameters.", http.StatusBadRequest)
		return
	}

	transactionCaptureReqBody := map[string]string{
		"posProfile": "ECOMMERCE",
	}

	transactionCaptureReqBodyJson, err := json.Marshal(transactionCaptureReqBody)
	if err != nil {
		log.Printf("Error encoding capture request body: %v", err)
		http.Error(w, "Something wrong. Please try again later.", http.StatusInternalServerError)
		return
	}

	transactionCaptureReq, err := http.NewRequest("POST", fmt.Sprintf("%v/api/v4/accounts/%v/payments/%v/capture",
		environmentUrl, accountId, params.TransactionId),
		bytes.NewBuffer(transactionCaptureReqBodyJson))
	if err != nil {
		log.Printf("Error setting a request to payment capture: %v", err)
		http.Error(w, "Something wrong. Please try again later.", http.StatusInternalServerError)
		return
	}

	transactionCaptureReq.Header.Set("Content-Type", "application/json")
	transactionCaptureReq.Header.Set("Authorization", fmt.Sprintf("Basic %v", basicToken))

	client := &http.Client{}
	transactionCaptureResp, err := client.Do(transactionCaptureReq)
	if err != nil {
		log.Printf("Error making a request to payment capture: %v", err)
		http.Error(w, "Something wrong. Please try again later.", http.StatusInternalServerError)
		return
	}
	defer transactionCaptureResp.Body.Close()

	transactionCaptureRespBody, err := io.ReadAll(transactionCaptureResp.Body)
	if err != nil {
		log.Printf("Error reading payment capture response: %v", err)
		http.Error(w, "Something wrong. Please try again later.", http.StatusInternalServerError)
		return
	}

	var handlerResponseBody Transaction
	if err = json.Unmarshal(transactionCaptureRespBody, &handlerResponseBody); err != nil {
		log.Printf("Error parsing payment capture response body: %v", err)
		http.Error(w, "Something wrong. Please try again later.", http.StatusInternalServerError)
		return
	}

	if _, err = app.DB.Exec("UPDATE transactions SET status = ?, details = ? WHERE transaction_id = ?",
		handlerResponseBody.Status, transactionCaptureRespBody, handlerResponseBody.TransactionId); err != nil {
		log.Printf("Error executing UPDATE query setting status to captured: %v", err)
		http.Error(w, "Something wrong. Please try again later.", http.StatusInternalServerError)
		return
	}

	handlerResponseBodyJson, err := json.Marshal(handlerResponseBody)
	if err != nil {
		log.Printf("Error encoding capture data: %v", err)
		http.Error(w, "Something wrong. Please try again later.", http.StatusInternalServerError)
		return
	}

	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(transactionCaptureResp.StatusCode)
	if _, err = w.Write(handlerResponseBodyJson); err != nil {
		log.Printf("Error writing capture data to connection: %v", err)
		http.Error(w, "Something wrong. Please try again later.", http.StatusInternalServerError)
		return
	}
}
