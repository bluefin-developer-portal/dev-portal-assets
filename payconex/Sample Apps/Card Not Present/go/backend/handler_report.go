package main

import (
	"encoding/json"
	"log"
	"net/http"
)

func (app *App) handlerGetReport(w http.ResponseWriter, r *http.Request) {
	type TransactionDetails struct {
		Details string
	}

	var transactionDetailsList []TransactionDetails
	var reportList []map[string]any

	detailsRows, err := app.DB.Query("SELECT details FROM transactions")
	if err != nil {
		log.Printf("Error getting the transactions details: %v", err)
		http.Error(w, "Something wrong. Please try again later.", http.StatusInternalServerError)
		return
	}
	defer detailsRows.Close()

	for detailsRows.Next() {
		transactionDetails := TransactionDetails{}
		if err = detailsRows.Scan(&transactionDetails.Details); err != nil {
			log.Printf("Error scanning details value: %v", err)
			http.Error(w, "Something wrong. Please try again later.", http.StatusInternalServerError)
			return
		}
		transactionDetailsList = append(transactionDetailsList, transactionDetails)
	}

	for _, transaction := range transactionDetailsList {
		var report map[string]any
		if err = json.Unmarshal([]byte(transaction.Details), &report); err != nil {
			log.Printf("Error parsing transaction details: %v", err)
			http.Error(w, "Something wrong. Please try again later.", http.StatusInternalServerError)
			return
		}
		reportList = append(reportList, report)
	}

	jsonData, err := json.Marshal(reportList)
	if err != nil {
		log.Printf("Error encoding report list: %v", err)
		http.Error(w, "Something wrong. Please try again later.", http.StatusInternalServerError)
		return
	}

	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(http.StatusOK)
	if _, err = w.Write(jsonData); err != nil {
		log.Printf("Error write JSON data of reports: %v", err)
		http.Error(w, "Something wrong. Please try again later.", http.StatusInternalServerError)
		return
	}
}
