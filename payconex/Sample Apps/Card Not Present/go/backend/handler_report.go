package main

import (
	"encoding/json"
	"log"
	"net/http"
)

func (app *App) handlerGetReport(w http.ResponseWriter, r *http.Request) {
	var report []map[string]any

	detailsRows, err := app.DB.Query("SELECT details FROM transactions")
	if err != nil {
		log.Printf("Error getting the transactions details: %v", err)
		http.Error(w, "Something wrong. Please try again later.", http.StatusInternalServerError)
		return
	}
	defer detailsRows.Close()

	for detailsRows.Next() {
		transactionDetails := ""
		if err = detailsRows.Scan(&transactionDetails); err != nil {
			log.Printf("Error scanning details value: %v", err)
			http.Error(w, "Something wrong. Please try again later.", http.StatusInternalServerError)
			return
		}

		var transaction_meta map[string]any
		if err = json.Unmarshal([]byte(transactionDetails), &transaction_meta); err != nil {
			log.Printf("Error parsing transaction details: %v", err)
			http.Error(w, "Something wrong. Please try again later.", http.StatusInternalServerError)
			return
		}
		report = append(report, transaction_meta)

	}

	jsonData, err := json.Marshal(report)
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
