package main

import (
  "log"
  "net/http"
  "encoding/json"
)


func handlerHealth(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json")

  out, err := json.Marshal("CHECK")

  if _, err = w.Write(out); err != nil {
    log.Printf("Error writing capture data to connection: %v", err)
    http.Error(w, "Something wrong. Please try again later.", http.StatusInternalServerError)
    return
  }

	w.WriteHeader(200)
	return
}
