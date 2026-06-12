-- Script d'initialisation pour la base de données bank_loan_db
-- Exécuté automatiquement par Hibernate avec ddl-auto: update

-- Les tables seront créées automatiquement par JPA/Hibernate
-- Ce fichier sert de documentation des structures de données

/*
CREATE TABLE demande_pret (
    id BIGSERIAL PRIMARY KEY,
    client_id BIGINT NOT NULL,
    montant_demande DECIMAL(15,2) NOT NULL,
    duree_mois INTEGER NOT NULL,
    motif VARCHAR(255),
    score_risque DECIMAL(5,2),
    statut VARCHAR(20) NOT NULL,
    date_soumission TIMESTAMP
);

CREATE TABLE pret (
    id BIGSERIAL PRIMARY KEY,
    demande_id BIGINT NOT NULL UNIQUE,
    client_id BIGINT NOT NULL,
    compte_id BIGINT NOT NULL,
    montant_accorde DECIMAL(15,2) NOT NULL,
    taux_interet DECIMAL(5,4) NOT NULL,
    duree_mois INTEGER NOT NULL,
    capital_restant DECIMAL(15,2) NOT NULL,
    statut VARCHAR(20) NOT NULL,
    date_deblocage TIMESTAMP
);

CREATE TABLE echeance (
    id BIGSERIAL PRIMARY KEY,
    pret_id BIGINT NOT NULL REFERENCES pret(id),
    numero INTEGER NOT NULL,
    date_echeance DATE NOT NULL,
    montant_capital DECIMAL(15,2) NOT NULL,
    montant_interet DECIMAL(15,2) NOT NULL,
    montant_total DECIMAL(15,2) NOT NULL,
    statut VARCHAR(20) NOT NULL
);

CREATE TABLE remboursement (
    id BIGSERIAL PRIMARY KEY,
    echeance_id BIGINT NOT NULL,
    montant DECIMAL(15,2) NOT NULL,
    date_paiement TIMESTAMP,
    moyen_paiement VARCHAR(20) NOT NULL
);

-- Index pour optimiser les requêtes
CREATE INDEX idx_demande_pret_client_id ON demande_pret(client_id);
CREATE INDEX idx_pret_client_id ON pret(client_id);
CREATE INDEX idx_echeance_pret_id ON echeance(pret_id);
CREATE INDEX idx_echeance_statut ON echeance(statut);
*/