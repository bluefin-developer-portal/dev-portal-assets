package main

import (
	"database/sql"
	"log"
	"net/http"
	"os"

	"github.com/go-chi/chi"
	"github.com/go-chi/cors"
	"github.com/joho/godotenv"
	_ "github.com/mattn/go-sqlite3"
)

type App struct {
	DB *sql.DB
}

type Transaction struct {
	TransactionId string
	Amount        string
	Status        string
}

func main() {
	err := os.Mkdir("./db", 0777)
	if err != nil {
		if !os.IsExist(err) {
			log.Fatal(err)
		}
	}

	db, err := sql.Open("sqlite3", "./db/transactions.db")
	// Confirm a successful connection.
	if err := db.Ping(); err != nil {
		log.Fatal(err)
	}
	defer db.Close()

	createTransactionTable := `CREATE TABLE IF NOT EXISTS transactions (
        id INTEGER PRIMARY KEY,
        transaction_id TEXT NOT NULL,
        amount TEXT NOT NULL,
        status TEXT NOT NULL,
        details TEXT NOT NULL
  )`

	if _, err = db.Exec(createTransactionTable); err != nil {
		log.Fatalf("Error creating transaction table: %v", err)
	}

	createCheckoutConfigTable := `CREATE TABLE IF NOT EXISTS checkout_config (
        id INTEGER PRIMARY KEY,
        resource_id TEXT NOT NULL,
        config TEXT NOT NULL
  )`

	if _, err = db.Exec(createCheckoutConfigTable); err != nil {
		log.Fatalf("Error creating checkout config table: %v", err)
	}

	app := &App{DB: db}

	router := chi.NewRouter()
	router.Use(cors.Handler(cors.Options{
		AllowedOrigins:   []string{"*"},
		AllowedMethods:   []string{"GET", "POST", "PATCH", "PUT", "DELETE", "OPTIONS"},
		AllowedHeaders:   []string{"*"},
		ExposedHeaders:   []string{"LINK"},
		AllowCredentials: false,
		MaxAge:           300,
	}))

	router.Get("/api", handlerHealth)
	router.Get("/api/report", app.handlerGetReport)
	router.Get("/api/config", app.handlerGetCheckoutConfigs)
	router.Post("/api/authorize-transaction", app.handlerProcessAuthorization)
	router.Post("/api/capture-transaction", app.handlerProcessCapture)
	router.Post("/api/refund-transaction", app.handlerProcessRefund)
	router.Post("/api/config", app.handlerGenerateConfig)
	router.Post("/api/generate-bearer-token", handlerGenerateBearerToken)
	router.Patch("/api/transaction/{transactionId}", app.handlerProcessUpdate)
	router.Delete("/api/transaction/{transactionId}", app.handlerDeleteTransaction)

	godotenv.Load()
	port := os.Getenv("PORT")
	if port == "" {
		log.Fatal("PORT not found in the environment")
	}

	server := &http.Server{
		Handler: router,
		Addr:    ":" + port,
	}

	log.Printf("Server starting on port %s", port)
	if err = server.ListenAndServe(); err != nil {
		log.Fatal(err)
	}
}
