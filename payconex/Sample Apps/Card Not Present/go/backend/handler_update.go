package main

import (
	"bytes"
	"encoding/json"
	"fmt"
	"io"
	"log"
	"net/http"
	"os"

	"github.com/go-chi/chi"
)

func (app *App) handlerProcessUpdate(w http.ResponseWriter, r *http.Request) {
	transactionId := chi.URLParam(r, "transactionId")

	accountId, basicToken, environmentUrl := os.Getenv("ACCOUNT_ID"),
		os.Getenv("BASIC_TOKEN"), "https://api-cert.payconex.net"

	reqBody, err := io.ReadAll(r.Body)
	if err != nil {
		log.Printf("Error reading update req body: %v", err)
		http.Error(w, "Something wrong. Please try again later.", http.StatusInternalServerError)
		return
	}

	transactionUpdateReq, err := http.NewRequest("PATCH", fmt.Sprintf("%v/api/v4/accounts/%v/payments/%v",
		environmentUrl, accountId, transactionId),
		bytes.NewBuffer(reqBody))

	transactionUpdateReq.Header.Set("Content-Type", "application/json")
	transactionUpdateReq.Header.Set("Authorization", fmt.Sprintf("Basic %v", basicToken))

	client := &http.Client{}
	transactionUpdateResp, err := client.Do(transactionUpdateReq)
	if err != nil {
		log.Printf("Error making a request to update transaction: %v", err)
		http.Error(w, "Something wrong. Please try again later.", http.StatusInternalServerError)
		return
	}
	defer transactionUpdateResp.Body.Close()

	transactionUpdateRespBody, err := io.ReadAll(transactionUpdateResp.Body)
	if err != nil {
		log.Printf("Error reading transaction update response: %v", err)
		http.Error(w, "Something wrong. Please try again later.", http.StatusInternalServerError)
		return
	}

	var handlerResponseBody map[string]any
	if err = json.Unmarshal(transactionUpdateRespBody, &handlerResponseBody); err != nil {
		log.Printf("Error parsing transaction update response body: %v", err)
		http.Error(w, "Something wrong. Please try again later.", http.StatusInternalServerError)
		return
	}

	encodedDetails, err := json.Marshal(handlerResponseBody)
	if err != nil {
		log.Printf("Error encoding transaction update details: %v", err)
		http.Error(w, "Something wrong. Please try again later.", http.StatusInternalServerError)
		return
	}

	if _, err = app.DB.Exec("UPDATE transactions SET  details = ? WHERE transaction_id = ?",
		encodedDetails, transactionId); err != nil {
		log.Printf("Error executing UPDATE of transaction update details: %v", err)
		http.Error(w, "Something wrong. Please try again later.", http.StatusInternalServerError)
		return
	}

	handlerResponseBodyJson, err := json.Marshal(handlerResponseBody)
	if err != nil {
		log.Printf("Error encoding update data: %v", err)
		http.Error(w, "Something wrong. Please try again later.", http.StatusInternalServerError)
		return
	}

	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(transactionUpdateResp.StatusCode)
	if _, err = w.Write(handlerResponseBodyJson); err != nil {
		log.Printf("Error writing update data to connection: %v", err)
		http.Error(w, "Something wrong. Please try again later.", http.StatusInternalServerError)
		return
	}
}
