-- Account Service - V1 Initial Schema
CREATE SCHEMA IF NOT EXISTS account_schema;

-- Ana hesaplar tablosu (JOINED inheritance base)
CREATE TABLE IF NOT EXISTS account_schema.accounts (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_type_disc   VARCHAR(20) NOT NULL,    -- JPA discriminator
    iban                VARCHAR(26) NOT NULL UNIQUE,
    balance             DECIMAL(15,2) NOT NULL DEFAULT 0,
    account_type        VARCHAR(20) NOT NULL,    -- VADESIZ, VADELI, TASARRUF, YATIRIM
    status              VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    owner_id            UUID NOT NULL,           -- auth-service kullanıcı ID (cross-service FK yok)
    currency            VARCHAR(3) NOT NULL DEFAULT 'TRY',
    account_name        VARCHAR(100),
    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP,
    created_by          VARCHAR(100),
    version             BIGINT DEFAULT 0         -- Optimistic locking: bakiye güncellemesi için kritik
);

-- Hızlı arama index'leri
CREATE INDEX IF NOT EXISTS idx_accounts_iban     ON account_schema.accounts(iban);
CREATE INDEX IF NOT EXISTS idx_accounts_owner_id ON account_schema.accounts(owner_id);
CREATE INDEX IF NOT EXISTS idx_accounts_status   ON account_schema.accounts(status);

-- Vadesiz hesap ek alanları
CREATE TABLE IF NOT EXISTS account_schema.vadesiz_hesaplar (
    id              UUID PRIMARY KEY REFERENCES account_schema.accounts(id) ON DELETE CASCADE,
    anlik_limit     DECIMAL(15,2) DEFAULT 50000.00,
    gunluk_limit    DECIMAL(15,2) DEFAULT 100000.00
);

-- Vadeli hesap ek alanları
CREATE TABLE IF NOT EXISTS account_schema.vadeli_hesaplar (
    id              UUID PRIMARY KEY REFERENCES account_schema.accounts(id) ON DELETE CASCADE,
    vade_gunu       INT NOT NULL,
    faiz_orani      DECIMAL(5,2) NOT NULL,
    vade_baslangic  DATE NOT NULL,
    vade_bitis      DATE NOT NULL
);

-- Tasarruf hesabı ek alanları
CREATE TABLE IF NOT EXISTS account_schema.tasarruf_hesaplar (
    id                      UUID PRIMARY KEY REFERENCES account_schema.accounts(id) ON DELETE CASCADE,
    aylik_faiz_orani        DECIMAL(5,2) DEFAULT 0.50,
    aylik_max_cekim         INT DEFAULT 6,
    bu_ay_cekim_sayisi      INT DEFAULT 0
);

-- Yatırım hesabı ek alanları
CREATE TABLE IF NOT EXISTS account_schema.yatirim_hesaplar (
    id              UUID PRIMARY KEY REFERENCES account_schema.accounts(id) ON DELETE CASCADE,
    risk_seviyesi   INT NOT NULL DEFAULT 1,
    portfoy_degeri  DECIMAL(15,2) DEFAULT 0,
    varliklar_json  TEXT
);
