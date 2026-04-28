-- V60__seed_financial_transactions.sql
-- Seeds 11 representative financial transactions for development/testing.
-- All linked to the first test case (lowest id, not deleted).
-- Dates sourced from VAA sheet (Excel serial converted to ISO dates).

INSERT INTO financial_transactions
    (case_id, direction, operation_type, payment_mode, amount, payment_date, description, created_at, updated_at)
SELECT
    c.id,
    t.direction::varchar,
    t.operation_type::varchar,
    t.payment_mode::varchar,
    t.amount,
    t.payment_date,
    t.description,
    NOW(),
    NOW()
FROM (
    VALUES
    ('REVENUE', 'OPENING_FEE',       'TRANSFER', 1500.00::numeric, '2024-03-26'::date, 'Honoraires affaire penale - Tribunal d''appel Rabat'),
    ('REVENUE', 'OPENING_FEE',       'TRANSFER', 1500.00,          '2024-06-15'::date, 'Honoraires affaire penale - Tribunal d''appel Rabat'),
    ('REVENUE', 'PROCEDURE_FEE',     'CHECK',     800.00,           '2024-02-16'::date, 'Frais procedure civile - استعجالي'),
    ('REVENUE', 'PROCEDURE_FEE',     'CASH',      600.00,           '2024-12-05'::date, 'Frais affaire civile - الأداء والإفراغ Rabat'),
    ('REVENUE', 'OPENING_FEE',       'TRANSFER', 1500.00,          '2024-03-10'::date, 'Honoraires affaire penale استئنافي'),
    ('REVENUE', 'PROCEDURE_FEE',     'TRANSFER',  700.00,           '2024-04-22'::date, 'Frais procedure استعجالي - Rabat 1ere instance'),
    ('REVENUE', 'INTERVENTION_FEE',  'CHECK',     900.00,           '2024-11-28'::date, 'Intervention عقار في طور التحفيظ'),
    ('REVENUE', 'DOCUMENT_FEE',      'CASH',      350.00,           '2024-05-05'::date, 'Documents التذييل بالصيغة التنفيذية اسرة'),
    ('REVENUE', 'PROCEDURE_FEE',     'TRANSFER',  600.00,           '2024-08-06'::date, 'Frais الأداء والإفراغ - Casablanca Appel'),
    ('REVENUE', 'OPENING_FEE',       'TRANSFER', 1500.00,          '2024-06-28'::date, 'Honoraires جنحي عادي استئنافي - Rabat Appel'),
    ('EXPENSE', 'JUDICIAL_TAX',      'TRANSFER',  200.00,           '2024-03-19'::date, 'Taxe judiciaire رسوم المحكمة')
) AS t(direction, operation_type, payment_mode, amount, payment_date, description)
CROSS JOIN (SELECT id FROM cases WHERE deleted_at IS NULL ORDER BY id LIMIT 1) AS c;
