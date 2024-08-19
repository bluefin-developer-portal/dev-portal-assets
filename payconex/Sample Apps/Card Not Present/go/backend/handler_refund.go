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

func getTransactionDetails(transactionId string) (map[string]any, error) {
	var details map[string]any

	accountId, basicToken, environmentUrl := os.Getenv("ACCOUNT_ID"),
		os.Getenv("BASIC_TOKEN"),
		"https://api-cert.payconex.net"

	transactionDetailsReq, err := http.NewRequest("GET", fmt.Sprintf("%v/api/v4/accounts/%v/payments/%v",
		environmentUrl, accountId, transactionId),
		bytes.NewBuffer([]byte{}))

	transactionDetailsReq.Header.Set("Content-Type", "application/json")
	transactionDetailsReq.Header.Set("Authorization", fmt.Sprintf("Basic %v", basicToken))

	client := &http.Client{}
	trasnsactionDetailsResp, err := client.Do(transactionDetailsReq)
	if err != nil {
		log.Printf("Error making a request to get transaction details: %v", err)
		return details, err
	}
	defer trasnsactionDetailsResp.Body.Close()

	transactionDetailsRespBody, err := io.ReadAll(trasnsactionDetailsResp.Body)
	if err != nil {
		log.Printf("Error reading transaction details response: %v", err)
		return details, err
	}

	if err = json.Unmarshal([]byte(transactionDetailsRespBody), &details); err != nil {
		log.Printf("Error parsing transaction details response body: %v", err)
		return details, err
	}
	return details, nil
}

func (app *App) handlerProcessRefund(w http.ResponseWriter, r *http.Request) {
	type RefundHandlerRequestBody struct {
		TransactionId string `validate:"required"`
	}

	var details map[string]any

	accountId, basicToken, environmentUrl := os.Getenv("ACCOUNT_ID"),
		os.Getenv("BASIC_TOKEN"), "https://api-cert.payconex.net"

	decoder, params := json.NewDecoder(r.Body), RefundHandlerRequestBody{}
	if err := decoder.Decode(&params); err != nil {
		log.Printf("Error decoding refund parameters: %v", err)
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

	transactionRefundReqBody := map[string]string{
		"posProfile": "ECOMMERCE",
	}

	transactionRefundReqBodyJson, err := json.Marshal(transactionRefundReqBody)
	if err != nil {
		log.Printf("Error encoding refund request body: %v", err)
		http.Error(w, "Something wrong. Please try again later.", http.StatusInternalServerError)
	}

	transactionRefundReq, err := http.NewRequest("POST", fmt.Sprintf("%v/api/v4/accounts/%v/payments/%v/refund",
		environmentUrl, accountId, params.TransactionId),
		bytes.NewBuffer(transactionRefundReqBodyJson))

	transactionRefundReq.Header.Set("Content-Type", "application/json")
	transactionRefundReq.Header.Set("Authorization", fmt.Sprintf("Basic %v", basicToken))

	client := &http.Client{}
	transactionRefundResp, err := client.Do(transactionRefundReq)
	if err != nil {
		log.Printf("Error making a request to payment refund: %v", err)
		http.Error(w, "Something wrong. Please try again later.", http.StatusInternalServerError)
		return
	}
	defer transactionRefundResp.Body.Close()

	transactionRefundRespBody, err := io.ReadAll(transactionRefundResp.Body)
	if err != nil {
		log.Printf("Error reading payment refund response: %v", err)
		http.Error(w, "Something wrong. Please try again later.", http.StatusInternalServerError)
		return
	}

	var handlerResponseBody Transaction
	if err = json.Unmarshal(transactionRefundRespBody, &handlerResponseBody); err != nil {
		log.Printf("Error parsing payment refund response body: %v", err)
		http.Error(w, "Something wrong. Please try again later.", http.StatusInternalServerError)
		return
	}

	details, err = getTransactionDetails(params.TransactionId)
	if err != nil {
		log.Printf("Error getting transaction details: %v", err)
		http.Error(w, "Something wrong. Please try again later.", http.StatusInternalServerError)
		return
	}

	encodedDetails, err := json.Marshal(details)
	if err != nil {
		log.Printf("Error encoding transaction details: %v", err)
		http.Error(w, "Something wrong. Please try again later.", http.StatusInternalServerError)
		return
	}

	if _, err = app.DB.Exec("UPDATE transactions SET status = ?, details = ? WHERE transaction_id = ?",
		handlerResponseBody.Status, encodedDetails, params.TransactionId); err != nil {
		log.Printf("Error executing UPDATE query setting status to voided: %v", err)
		http.Error(w, "Something wrong. Please try again later.", http.StatusInternalServerError)
		return
	}

	handlerResponseBodyJson, err := json.Marshal(handlerResponseBody)
	if err != nil {
		log.Printf("Error encoding refund data: %v", err)
		http.Error(w, "Something wrong. Please try again later.", http.StatusInternalServerError)
		return
	}

	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(http.StatusOK)
	if _, err = w.Write(handlerResponseBodyJson); err != nil {
		log.Printf("Error writing refund data to connection: %v", err)
		http.Error(w, "Something wrong. Please try again later.", http.StatusInternalServerError)
		return
	}
}
