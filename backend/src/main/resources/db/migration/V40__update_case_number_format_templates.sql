-- V40: Update case number format templates to {CASETYPE}/{TRIBUNAL_CODE}/{YEAR}/{SEQ5}
-- Previous format was {YEAR}-{TRIBUNAL_CODE}-{CASETYPE}-{SEQ5} (V20)

UPDATE case_types
SET number_format_template = '{CASETYPE}/{TRIBUNAL_CODE}/{YEAR}/{SEQ5}'
WHERE code IN ('CIVIL', 'PENAL', 'COMMERC', 'ADM', 'SOCIAL');
