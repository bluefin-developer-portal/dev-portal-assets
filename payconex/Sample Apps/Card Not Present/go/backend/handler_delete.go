package main

import (
	"log"
	"net/http"

	"github.com/go-chi/chi"
)

func (app *App) handlerDeleteTransaction(w http.ResponseWriter, r *http.Request) {
	transactionId := chi.URLParam(r, "transactionId")

	if _, err := app.DB.Exec("DELETE FROM transactions WHERE transaction_id = ?",
		transactionId); err != nil {
		log.Printf("Error executing DELETE query for transaction: %v", err)
		http.Error(w, "Something wrong. Please try again later.", http.StatusInternalServerError)
		return
	}

	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(http.StatusNoContent)
	return
}
