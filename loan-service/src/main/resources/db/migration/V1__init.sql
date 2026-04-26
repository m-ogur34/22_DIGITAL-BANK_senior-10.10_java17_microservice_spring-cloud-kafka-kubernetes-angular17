-- Loan Service - V1 Initial Schema
CREATE SCHEMA IF NOT EXISTS loan_schema;

CREATE TABLE IF NOT EXISTS loan_schema.loan_applications (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id            UUID NOT NULL,
    owner_iban          VARCHAR(26) NOT NULL,
    loan_type           VARCHAR(20) NOT NULL,
    requested_amount    DECIMAL(15,2) NOT NULL,
    approved_amount     DECIMAL(15,2),
    term_months         INT NOT NULL,
    annual_interest_rate DECIMAL(5,2),
    monthly_installment DECIMAL(15,2),
    total_payment       DECIMAL(15,2),
    status              VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    credit_score        INT,
    rejection_reason    VARCHAR(500),
    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP,
    created_by          VARCHAR(100),
    version             BIGINT DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_loan_owner_id ON loan_schema.loan_applications(owner_id);
CREATE INDEX IF NOT EXISTS idx_loan_status   ON loan_schema.loan_applications(status);

CREATE TABLE IF NOT EXISTS loan_schema.installments (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    loan_application_id  UUID NOT NULL REFERENCES loan_schema.loan_applications(id) ON DELETE CASCADE,
    installment_number   INT NOT NULL,
    due_date             DATE NOT NULL,
    amount               DECIMAL(15,2) NOT NULL,
    principal_amount     DECIMAL(15,2) NOT NULL,
    interest_amount      DECIMAL(15,2) NOT NULL,
    remaining_principal  DECIMAL(15,2),
    is_paid              BOOLEAN NOT NULL DEFAULT FALSE,
    payment_date         DATE,
    created_at           TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMP,
    created_by           VARCHAR(100),
    version              BIGINT DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_installment_loan_id  ON loan_schema.installments(loan_application_id);
CREATE INDEX IF NOT EXISTS idx_installment_due_date ON loan_schema.installments(due_date);
