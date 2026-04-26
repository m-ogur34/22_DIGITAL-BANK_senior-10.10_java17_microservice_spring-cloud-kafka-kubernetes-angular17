-- Loan Service - V2 Seed Data
INSERT INTO loan_schema.loan_applications
    (id, owner_id, owner_iban, loan_type, requested_amount, approved_amount,
     term_months, annual_interest_rate, monthly_installment, total_payment, status, credit_score, created_at, version)
VALUES (
    'g0000001-0000-0000-0000-000000000001',
    'c0000001-0000-0000-0000-000000000002',  -- Ayşe Demir
    'TR330006100098765432109876',
    'IHTIYAC',
    50000.00,
    50000.00,
    24,
    30.00,
    2778.50,
    66684.00,
    'APPROVED',
    720,
    NOW() - INTERVAL '30 days',
    0
) ON CONFLICT (id) DO NOTHING;
