-- ================================================================
-- Auth Service - V2 Seed Data
-- Test ve geliştirme ortamı için başlangıç verileri
-- ================================================================

-- ── Roller ──
INSERT INTO auth_schema.roles (id, name) VALUES
    ('a0000001-0000-0000-0000-000000000001', 'ROLE_CUSTOMER'),
    ('a0000001-0000-0000-0000-000000000002', 'ROLE_EMPLOYEE'),
    ('a0000001-0000-0000-0000-000000000003', 'ROLE_ADMIN')
ON CONFLICT (name) DO NOTHING;

-- ── Admin Kullanıcıları ──
-- Şifre: Admin123! → BCrypt hash (work factor 10)
-- Hash üretimi: BCryptPasswordEncoder(10).encode("Admin123!")
INSERT INTO auth_schema.users (id, user_type, email, password_hash, first_name, last_name, is_active, is_email_verified, created_at, version)
VALUES
    (
        'b0000001-0000-0000-0000-000000000001',
        'EMPLOYEE',
        'admin@digitalbank.com',
        '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
        'Sistem',
        'Yönetici',
        true,
        true,
        NOW(),
        0
    ),
    (
        'b0000001-0000-0000-0000-000000000002',
        'EMPLOYEE',
        'admin2@digitalbank.com',
        '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
        'İkinci',
        'Admin',
        true,
        true,
        NOW(),
        0
    )
ON CONFLICT (email) DO NOTHING;

-- Admin çalışan kayıtları
INSERT INTO auth_schema.employees (id, sicil_no, department, authority_level)
VALUES
    ('b0000001-0000-0000-0000-000000000001', 'EMP001', 'BILGI_TEKNOLOJILERI', 'MANAGER'),
    ('b0000001-0000-0000-0000-000000000002', 'EMP002', 'BILGI_TEKNOLOJILERI', 'SENIOR')
ON CONFLICT (id) DO NOTHING;

-- Admin roller ataması
INSERT INTO auth_schema.user_roles (user_id, role_id)
VALUES
    ('b0000001-0000-0000-0000-000000000001', 'a0000001-0000-0000-0000-000000000003'),
    ('b0000001-0000-0000-0000-000000000001', 'a0000001-0000-0000-0000-000000000002'),
    ('b0000001-0000-0000-0000-000000000002', 'a0000001-0000-0000-0000-000000000003')
ON CONFLICT DO NOTHING;

-- ── Müşteri Kullanıcıları ──
-- Şifre: Customer123! → BCrypt hash
INSERT INTO auth_schema.users (id, user_type, email, password_hash, first_name, last_name, is_active, is_email_verified, created_at, version)
VALUES
    (
        'c0000001-0000-0000-0000-000000000001',
        'CUSTOMER',
        'ali.kaya@example.com',
        '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
        'Ali',
        'Kaya',
        true,
        true,
        NOW(),
        0
    ),
    (
        'c0000001-0000-0000-0000-000000000002',
        'CUSTOMER',
        'ayse.demir@example.com',
        '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
        'Ayşe',
        'Demir',
        true,
        true,
        NOW(),
        0
    ),
    (
        'c0000001-0000-0000-0000-000000000003',
        'CUSTOMER',
        'mehmet.yilmaz@example.com',
        '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
        'Mehmet',
        'Yılmaz',
        true,
        true,
        NOW(),
        0
    )
ON CONFLICT (email) DO NOTHING;

-- Müşteri ek bilgileri
-- TC kimlik numaraları: algoritmaya uygun gerçek format (test değerleri)
INSERT INTO auth_schema.customers (id, tc_no, customer_no, phone, monthly_income)
VALUES
    ('c0000001-0000-0000-0000-000000000001', '11111111110', 'DB00000001', '05551234567', 15000.00),
    ('c0000001-0000-0000-0000-000000000002', '22222222226', 'DB00000002', '05559876543', 25000.00),
    ('c0000001-0000-0000-0000-000000000003', '33333333332', 'DB00000003', '05551112233', 8000.00)
ON CONFLICT (id) DO NOTHING;

-- Müşteri rol atamaları
INSERT INTO auth_schema.user_roles (user_id, role_id)
VALUES
    ('c0000001-0000-0000-0000-000000000001', 'a0000001-0000-0000-0000-000000000001'),
    ('c0000001-0000-0000-0000-000000000002', 'a0000001-0000-0000-0000-000000000001'),
    ('c0000001-0000-0000-0000-000000000003', 'a0000001-0000-0000-0000-000000000001')
ON CONFLICT DO NOTHING;
