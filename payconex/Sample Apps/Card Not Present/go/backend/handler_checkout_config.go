package main

import (
	"encoding/json"
	"log"
	"net/http"
)

func (app *App) handlerGetCheckoutConfigs(w http.ResponseWriter, r *http.Request) {
	type CheckoutConfig struct {
		ResourceId string `json:"resourceId"`
	}

	var checkoutConfigList []CheckoutConfig

	checkoutConfigRows, err := app.DB.Query("SELECT resource_id FROM checkout_config")
	if err != nil {
		log.Printf("Error getting the checkout configs: %v", err)
		http.Error(w, "Something wrong. Please try again later.", http.StatusInternalServerError)
		return
	}
	defer checkoutConfigRows.Close()

	for checkoutConfigRows.Next() {
		checkoutConfig := CheckoutConfig{}
		if err = checkoutConfigRows.Scan(&checkoutConfig.ResourceId); err != nil {
			log.Printf("Error scanning db values: %v", err)
			http.Error(w, "Something wrong. Please try again later.", http.StatusInternalServerError)
			return
		}
		checkoutConfigList = append(checkoutConfigList, checkoutConfig)
	}

	jsonData, err := json.Marshal(checkoutConfigList)
	if err != nil {
		log.Printf("Error marshal checkout config list: %v", err)
		http.Error(w, "Something wrong. Please try again later.", http.StatusInternalServerError)
		return
	}

	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(http.StatusOK)
	if _, err = w.Write(jsonData); err != nil {
		log.Printf("Error write JSON data: %v", err)
		http.Error(w, "Something wrong. Please try again later.", http.StatusInternalServerError)
		return
	}
}
