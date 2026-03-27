-- OmniCharge — PostgreSQL DB initialisation
-- This script runs once when the postgres container starts for the first time.
-- It creates a separate database for each microservice (12-Factor App: backing services).

CREATE DATABASE user_db;
CREATE DATABASE operator_db;
CREATE DATABASE recharge_db;
CREATE DATABASE payment_db;
