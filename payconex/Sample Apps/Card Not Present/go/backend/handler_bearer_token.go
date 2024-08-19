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

func handlerGenerateBearerToken(w http.ResponseWriter, r *http.Request) {
	type BearerHandlerRequestBody struct {
		Amount     string `validate:"required"`
		ResourceId string `validate:"required"`
	}

	type BearerHandlerResponseBody struct {
		BearerToken string `json:"bearerToken"`
	}

	accountId, basicToken, templateReference, environmentUrl := os.Getenv("ACCOUNT_ID"),
		os.Getenv("BASIC_TOKEN"), os.Getenv("TEMPLATE_REFERENCE"), "https://api-cert.payconex.net"

	decoder, params := json.NewDecoder(r.Body), BearerHandlerRequestBody{}
	if err := decoder.Decode(&params); err != nil {
		log.Printf("Error decoding bearer token parameters: %v", err)
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

	instanceRequestBody := map[string]any{
		"label":     "my-instance-2",
		"amount":    params.Amount,
		"reference": templateReference,
		"threeDSecureInitSettings": map[string]string{
			"transactionType":                "GOODS_SERVICE_PURCHASE",
			"deliveryTimeFrame":              "ELECTRONIC_DELIVERY",
			"threeDSecureChallengeIndicator": "NO_PREFERENCE",
			"reorderIndicator":               "FIRST_TIME_ORDERED",
			"shippingIndicator":              "BILLING_ADDRESS",
		},
	}

	instanceRequestBodyJson, err := json.Marshal(instanceRequestBody)
	if err != nil {
		log.Printf("Error encoding instance request body: %v", err)
		http.Error(w, "Something wrong. Please try again later.", http.StatusInternalServerError)
		return
	}

	instanceRequest, err := http.NewRequest("POST", fmt.Sprintf("%v/api/v4/accounts/%v/payment-iframe/%v/instance/init",
		environmentUrl, accountId, params.ResourceId),
		bytes.NewBuffer(instanceRequestBodyJson))
	if err != nil {
		log.Printf("Error setting request to instance init: %v", err)
		http.Error(w, "Something wrong. Please try again later.", http.StatusInternalServerError)
		return
	}

	instanceRequest.Header.Set("Content-Type", "application/json")
	instanceRequest.Header.Set("Authorization", fmt.Sprintf("Basic %v", basicToken))

	client := &http.Client{}
	instanceRespponse, err := client.Do(instanceRequest)
	if err != nil {
		log.Printf("Error making request to instance init: %v", err)
		http.Error(w, "Something wrong. Please try again later.", http.StatusInternalServerError)
		return
	}
	defer instanceRespponse.Body.Close()

	instanceResponseBody, err := io.ReadAll(instanceRespponse.Body)
	if err != nil {
		log.Printf("Error reading instance init response: %v", err)
		http.Error(w, "Something wrong. Please try again later.", http.StatusInternalServerError)
		return
	}

	var handlerResponseBody BearerHandlerResponseBody
	if err = json.Unmarshal(instanceResponseBody, &handlerResponseBody); err != nil {
		log.Printf("Error parsing instance response body: %v", err)
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
	w.WriteHeader(instanceRespponse.StatusCode)
	if _, err = w.Write(handlerResponseBodyJson); err != nil {
		log.Printf("Error writing bearerToken body to connection: %v", err)
		http.Error(w, "Something wrong. Please try again later.", http.StatusInternalServerError)
		return
	}
}
