package main

import (
	"bytes"
	"encoding/json"
	"fmt"
	"github.com/go-playground/validator/v10"
	_ "github.com/mattn/go-sqlite3"
	"io"
	"log"
	"net/http"
	"os"
)

func (app *App) handlerProcessAuthorization(w http.ResponseWriter, r *http.Request) {
	type AuthHandlerRequestBody struct {
		Amount        string `validate:"required"`
		PayConexToken string `validate:"required"`
	}

	accountId, basicToken, environmentUrl := os.Getenv("ACCOUNT_ID"),
		os.Getenv("BASIC_TOKEN"), "https://api-cert.payconex.net"

	decoder, params := json.NewDecoder(r.Body), AuthHandlerRequestBody{}
	if err := decoder.Decode(&params); err != nil {
		log.Printf("Error decoding auth parameters: %v", err)
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

	transactionAuthRequestBody := map[string]any{
		"token":      params.PayConexToken,
		"posProfile": "ECOMMERCE",
		"amounts": map[string]string{
			"total":    params.Amount,
			"currency": "USD",
		},
	}

	transactionAuthBodyJson, err := json.Marshal(transactionAuthRequestBody)
	if err != nil {
		log.Printf("Error encoding auth request body: %v", err)
		http.Error(w, "Something wrong. Please try again later.", http.StatusInternalServerError)
		return
	}

	transactionAuthReq, err := http.NewRequest("POST", fmt.Sprintf("%v/api/v4/accounts/%v/payments/auth",
		environmentUrl, accountId),
		bytes.NewBuffer(transactionAuthBodyJson))
	if err != nil {
		log.Printf("Error setting a request to payment auth: %v", err)
		http.Error(w, "Something wrong. Please try again later.", http.StatusInternalServerError)
		return
	}

	transactionAuthReq.Header.Set("Content-Type", "application/json")
	transactionAuthReq.Header.Set("Authorization", fmt.Sprintf("Basic %v", basicToken))

	client := &http.Client{}
	transactionAuthResp, err := client.Do(transactionAuthReq)
	if err != nil {
		log.Printf("Error making a request to payment auth: %v", err)
		http.Error(w, "Something wrong. Please try again later.", http.StatusInternalServerError)
		return
	}
	defer transactionAuthResp.Body.Close()

	transactionAuthRespBody, err := io.ReadAll(transactionAuthResp.Body)
	if err != nil {
		log.Printf("Error reading payment auth response: %v", err)
		http.Error(w, "Something wrong. Please try again later.", http.StatusInternalServerError)
		return
	}

	var handlerResponseBody Transaction
	if err = json.Unmarshal(transactionAuthRespBody, &handlerResponseBody); err != nil {
		log.Printf("Error parsing payment auth response body: %v", err)
		http.Error(w, "Something wrong. Please try again later.", http.StatusInternalServerError)
		return
	}

	if _, err = app.DB.Exec("INSERT INTO transactions(transaction_id, amount, status, details) VALUES ( ?, ?, ?, ?)",
		handlerResponseBody.TransactionId, handlerResponseBody.TransactionId, handlerResponseBody.Status, transactionAuthRespBody); err != nil {
		log.Printf("Error executing INSERT query into transactions: %v", err)
		http.Error(w, "Something wrong. Please try again later.", http.StatusInternalServerError)
		return
	}

	handlerResponseBodyJson, err := json.Marshal(handlerResponseBody)
	if err != nil {
		log.Printf("Error encoding auth data: %v", err)
		http.Error(w, "Something wrong. Please try again later.", http.StatusInternalServerError)
		return
	}

	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(transactionAuthResp.StatusCode)
	if _, err := w.Write(handlerResponseBodyJson); err != nil {
		log.Printf("Error writing auth data to connection: %v", err)
		http.Error(w, "Something wrong. Please try again later.", http.StatusInternalServerError)
		return
	}
}
