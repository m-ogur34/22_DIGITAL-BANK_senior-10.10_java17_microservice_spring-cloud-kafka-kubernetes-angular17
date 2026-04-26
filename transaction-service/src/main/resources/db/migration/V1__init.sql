-- Transaction Service - V1 Initial Schema
CREATE SCHEMA IF NOT EXISTS transaction_schema;

CREATE TABLE IF NOT EXISTS transaction_schema.transactions (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sender_iban             VARCHAR(26) NOT NULL,
    receiver_iban           VARCHAR(26) NOT NULL,
    amount                  DECIMAL(15,2) NOT NULL,
    description             VARCHAR(255),
    type                    VARCHAR(30) NOT NULL,    -- DEBIT, CREDIT, REVERSAL, EXTERNAL_TRANSFER
    status                  VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    owner_id                VARCHAR(36) NOT NULL,    -- İşlemi yapan kullanıcı UUID
    reference_id            VARCHAR(36),             -- Debit+Credit çiftini bağlar
    reversed_transaction_id VARCHAR(36),             -- Reversal için orijinal işlem ID
    failure_reason          VARCHAR(500),
    is_internal             BOOLEAN NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMP,
    created_by              VARCHAR(100),
    version                 BIGINT DEFAULT 0
);

-- Sık sorgulanan alanlara index
CREATE INDEX IF NOT EXISTS idx_txn_sender_iban   ON transaction_schema.transactions(sender_iban);
CREATE INDEX IF NOT EXISTS idx_txn_receiver_iban ON transaction_schema.transactions(receiver_iban);
CREATE INDEX IF NOT EXISTS idx_txn_owner_id      ON transaction_schema.transactions(owner_id);
CREATE INDEX IF NOT EXISTS idx_txn_reference_id  ON transaction_schema.transactions(reference_id);
CREATE INDEX IF NOT EXISTS idx_txn_status        ON transaction_schema.transactions(status);
CREATE INDEX IF NOT EXISTS idx_txn_created_at    ON transaction_schema.transactions(created_at DESC);
