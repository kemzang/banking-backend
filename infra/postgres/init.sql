-- Cree une base de donnees dediee par microservice (principe "database per service").
-- Execute automatiquement au premier demarrage du conteneur PostgreSQL.

CREATE DATABASE bank_customer_db;
CREATE DATABASE bank_account_db;
CREATE DATABASE bank_transaction_db;
CREATE DATABASE bank_loan_db;
