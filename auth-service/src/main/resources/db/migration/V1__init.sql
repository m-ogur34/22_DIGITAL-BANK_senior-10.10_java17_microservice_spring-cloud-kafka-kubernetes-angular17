-- ================================================================
-- Auth Service - V1 Initial Schema
-- Flyway migration: her çalıştırmada otomatik uygulanır
-- Versiyonlar sırayla çalışır: V1 → V2 → V3 ...
-- ================================================================

-- Schema oluştur (yoksa)
CREATE SCHEMA IF NOT EXISTS auth_schema;

-- ── Roller tablosu ──
-- Spring Security roller burada saklanır: ROLE_CUSTOMER, ROLE_EMPLOYEE, ROLE_ADMIN
CREATE TABLE IF NOT EXISTS auth_schema.roles (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(30) NOT NULL UNIQUE
);

-- ── Kullanıcılar tablosu (JOINED inheritance base tablo) ──
-- Tüm kullanıcı tipleri (Customer, Employee) ortak alanlarını buraya yazar
CREATE TABLE IF NOT EXISTS auth_schema.users (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_type        VARCHAR(20) NOT NULL,       -- JPA discriminator: CUSTOMER / EMPLOYEE
    email            VARCHAR(100) NOT NULL UNIQUE,
    password_hash    VARCHAR(255) NOT NULL,       -- BCrypt hash
    first_name       VARCHAR(50) NOT NULL,
    last_name        VARCHAR(50) NOT NULL,
    is_active        BOOLEAN NOT NULL DEFAULT TRUE,
    is_email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    created_at       TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP,
    created_by       VARCHAR(100),
    version          BIGINT DEFAULT 0            -- Optimistic locking için
);

-- Email'e göre hızlı arama için index
CREATE INDEX IF NOT EXISTS idx_users_email ON auth_schema.users(email);

-- ── Müşteriler tablosu (Customer alt sınıfı) ──
-- JOINED inheritance: users tablosuyla JOIN edilerek tam Customer elde edilir
CREATE TABLE IF NOT EXISTS auth_schema.customers (
    id              UUID PRIMARY KEY REFERENCES auth_schema.users(id) ON DELETE CASCADE,
    tc_no           VARCHAR(11) NOT NULL UNIQUE,
    customer_no     VARCHAR(20) NOT NULL UNIQUE,
    phone           VARCHAR(15),
    monthly_income  DECIMAL(15,2)
);

-- TC No ve müşteri numarasına göre hızlı arama
CREATE INDEX IF NOT EXISTS idx_customers_tc_no       ON auth_schema.customers(tc_no);
CREATE INDEX IF NOT EXISTS idx_customers_customer_no ON auth_schema.customers(customer_no);

-- ── Çalışanlar tablosu (Employee alt sınıfı) ──
CREATE TABLE IF NOT EXISTS auth_schema.employees (
    id              UUID PRIMARY KEY REFERENCES auth_schema.users(id) ON DELETE CASCADE,
    sicil_no        VARCHAR(20) NOT NULL UNIQUE,
    department      VARCHAR(50) NOT NULL,
    authority_level VARCHAR(20) NOT NULL
);

-- ── Kullanıcı-Rol ilişki tablosu (çoka çok) ──
CREATE TABLE IF NOT EXISTS auth_schema.user_roles (
    user_id UUID REFERENCES auth_schema.users(id) ON DELETE CASCADE,
    role_id UUID REFERENCES auth_schema.roles(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)  -- Bileşik anahtar: aynı rol iki kez atanamaz
);

-- ── Refresh token'lar tablosu ──
CREATE TABLE IF NOT EXISTS auth_schema.refresh_tokens (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    token       TEXT NOT NULL UNIQUE,           -- JWT refresh token string'i
    user_id     UUID NOT NULL REFERENCES auth_schema.users(id) ON DELETE CASCADE,
    expiry_date TIMESTAMP NOT NULL,
    is_revoked  BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user_id ON auth_schema.refresh_tokens(user_id);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_token   ON auth_schema.refresh_tokens(token);
