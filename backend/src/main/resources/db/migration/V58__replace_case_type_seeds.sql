-- Upsert existing 4 case types with corrected bilingual names.
-- Codes are KEPT as-is (PENAL, COMMERC, CIVIL, ADM) because cases
-- already reference them via FK. Only names are updated.
-- Uses standard SQL MERGE (ISO SQL:2003) compatible with H2 2.x and PostgreSQL 15+.

MERGE INTO case_types AS t
USING (VALUES
('CIVIL',   'Civile',          'مدني',  '{YEAR}-{TRIBUNAL_CODE}-{CASETYPE}-{SEQ5}', TRUE),
('PENAL',   'Pénale',          'جنائي', '{YEAR}-{TRIBUNAL_CODE}-{CASETYPE}-{SEQ5}', TRUE),
('COMMERC', 'Commerciale',     'تجاري', '{YEAR}-{TRIBUNAL_CODE}-{CASETYPE}-{SEQ5}', TRUE),
('ADM',     'Administrative',  'إداري', '{YEAR}-{TRIBUNAL_CODE}-{CASETYPE}-{SEQ5}', TRUE)
) AS s (code, name_fr, name_ar, number_format_template, active)
ON t.code = s.code
WHEN MATCHED THEN UPDATE SET
    name_fr                = s.name_fr,
    name_ar                = s.name_ar,
    number_format_template = s.number_format_template,
    active                 = s.active
WHEN NOT MATCHED THEN INSERT (code, name_fr, name_ar, number_format_template, active)
    VALUES (s.code, s.name_fr, s.name_ar, s.number_format_template, s.active);
