-- Account Service - V2 Seed Data
-- auth-service seed data'sındaki kullanıcı ID'leri ile eşleşir

-- Ali Kaya'nın vadesiz hesabı
INSERT INTO account_schema.accounts (id, account_type_disc, iban, balance, account_type, status, owner_id, currency, account_name, version)
VALUES (
    'e0000001-0000-0000-0000-000000000001',
    'VADESIZ',
    'TR330006100012345678901234',
    5000.00,
    'VADESIZ',
    'ACTIVE',
    'c0000001-0000-0000-0000-000000000001',  -- Ali Kaya
    'TRY',
    'Ana Hesabım',
    0
) ON CONFLICT (iban) DO NOTHING;

INSERT INTO account_schema.vadesiz_hesaplar (id, anlik_limit, gunluk_limit)
VALUES ('e0000001-0000-0000-0000-000000000001', 50000.00, 100000.00)
ON CONFLICT (id) DO NOTHING;

-- Ayşe Demir'in vadesiz hesabı
INSERT INTO account_schema.accounts (id, account_type_disc, iban, balance, account_type, status, owner_id, currency, account_name, version)
VALUES (
    'e0000001-0000-0000-0000-000000000002',
    'VADESIZ',
    'TR330006100098765432109876',
    12500.00,
    'VADESIZ',
    'ACTIVE',
    'c0000001-0000-0000-0000-000000000002',  -- Ayşe Demir
    'TRY',
    'Vadesiz Hesabım',
    0
) ON CONFLICT (iban) DO NOTHING;

INSERT INTO account_schema.vadesiz_hesaplar (id, anlik_limit, gunluk_limit)
VALUES ('e0000001-0000-0000-0000-000000000002', 50000.00, 100000.00)
ON CONFLICT (id) DO NOTHING;

-- Ayşe Demir'in vadeli hesabı
INSERT INTO account_schema.accounts (id, account_type_disc, iban, balance, account_type, status, owner_id, currency, account_name, version)
VALUES (
    'e0000001-0000-0000-0000-000000000003',
    'VADELI',
    'TR440006100011122233344455',
    50000.00,
    'VADELI',
    'ACTIVE',
    'c0000001-0000-0000-0000-000000000002',  -- Ayşe Demir
    'TRY',
    '90 Günlük Vadeli',
    0
) ON CONFLICT (iban) DO NOTHING;

INSERT INTO account_schema.vadeli_hesaplar (id, vade_gunu, faiz_orani, vade_baslangic, vade_bitis)
VALUES (
    'e0000001-0000-0000-0000-000000000003',
    90,
    12.50,
    CURRENT_DATE,
    CURRENT_DATE + INTERVAL '90 days'
) ON CONFLICT (id) DO NOTHING;

-- Mehmet Yılmaz'ın tasarruf hesabı
INSERT INTO account_schema.accounts (id, account_type_disc, iban, balance, account_type, status, owner_id, currency, account_name, version)
VALUES (
    'e0000001-0000-0000-0000-000000000004',
    'TASARRUF',
    'TR550006100055566677788899',
    3200.00,
    'TASARRUF',
    'ACTIVE',
    'c0000001-0000-0000-0000-000000000003',  -- Mehmet Yılmaz
    'TRY',
    'Birikim Hesabım',
    0
) ON CONFLICT (iban) DO NOTHING;

INSERT INTO account_schema.tasarruf_hesaplar (id, aylik_faiz_orani, aylik_max_cekim, bu_ay_cekim_sayisi)
VALUES ('e0000001-0000-0000-0000-000000000004', 0.50, 6, 0)
ON CONFLICT (id) DO NOTHING;
