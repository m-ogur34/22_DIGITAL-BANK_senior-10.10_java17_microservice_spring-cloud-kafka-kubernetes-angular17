-- Transaction Service - V2 Seed Data
-- Örnek işlem geçmişi — account-service seed data'sındaki IBAN'lar ile eşleşir

INSERT INTO transaction_schema.transactions
    (id, sender_iban, receiver_iban, amount, description, type, status, owner_id, reference_id, is_internal, created_at, version)
VALUES
    -- Ali Kaya → Ayşe Demir transfer (DEBIT kaydı)
    (
        'f0000001-0000-0000-0000-000000000001',
        'TR330006100012345678901234',
        'TR330006100098765432109876',
        500.00,
        'Kira ödemesi',
        'DEBIT',
        'COMPLETED',
        'c0000001-0000-0000-0000-000000000001',
        'ref-001-ali-ayse',
        true,
        NOW() - INTERVAL '5 days',
        0
    ),
    -- Ali Kaya → Ayşe Demir transfer (CREDIT kaydı)
    (
        'f0000001-0000-0000-0000-000000000002',
        'TR330006100012345678901234',
        'TR330006100098765432109876',
        500.00,
        'Kira ödemesi',
        'CREDIT',
        'COMPLETED',
        'c0000001-0000-0000-0000-000000000001',
        'ref-001-ali-ayse',
        true,
        NOW() - INTERVAL '5 days',
        0
    ),
    -- Ayşe Demir → Mehmet Yılmaz transfer
    (
        'f0000001-0000-0000-0000-000000000003',
        'TR330006100098765432109876',
        'TR550006100055566677788899',
        250.00,
        'Market alışverişi',
        'DEBIT',
        'COMPLETED',
        'c0000001-0000-0000-0000-000000000002',
        'ref-002-ayse-mehmet',
        true,
        NOW() - INTERVAL '2 days',
        0
    ),
    (
        'f0000001-0000-0000-0000-000000000004',
        'TR330006100098765432109876',
        'TR550006100055566677788899',
        250.00,
        'Market alışverişi',
        'CREDIT',
        'COMPLETED',
        'c0000001-0000-0000-0000-000000000002',
        'ref-002-ayse-mehmet',
        true,
        NOW() - INTERVAL '2 days',
        0
    )
ON CONFLICT (id) DO NOTHING;
