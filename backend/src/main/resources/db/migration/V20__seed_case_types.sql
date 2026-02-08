INSERT INTO case_types (code, name_fr, name_ar, number_format_template) VALUES
('PENAL', 'Pénale', 'جنائي', '{YEAR}-{TRIBUNAL_CODE}-{CASETYPE}-{SEQ5}'),
('COMMERC', 'Commerciale', 'تجاري', '{YEAR}-{TRIBUNAL_CODE}-{CASETYPE}-{SEQ5}'),
('CIVIL', 'Civile', 'مدني', '{YEAR}-{TRIBUNAL_CODE}-{CASETYPE}-{SEQ5}'),
('ADM', 'Administrative', 'إداري', '{YEAR}-{TRIBUNAL_CODE}-{CASETYPE}-{SEQ5}');
