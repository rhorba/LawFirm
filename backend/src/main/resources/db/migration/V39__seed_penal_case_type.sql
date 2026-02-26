-- V39: Ensure PENAL case type has bilingual labels, categories, and status mappings
-- Guards against duplication since PENAL was already seeded in V20/V22/V26

-- 1. Insert PENAL case type only if missing
INSERT INTO case_types (code, name_fr, name_ar, number_format_template, active)
SELECT 'PENAL', 'Pénal', 'جنائي', 'PENAL/{TRIBUNAL}/{YEAR}/{SEQUENCE}', true
WHERE NOT EXISTS (SELECT 1 FROM case_types WHERE code = 'PENAL');

-- 2. Insert PENAL categories only if missing
INSERT INTO case_categories (code, name_ar, name_fr, case_type_id)
SELECT '2101', 'جنحي عادي تأديبي', 'Correctionnel ordinaire', ct.id
FROM case_types ct
WHERE ct.code = 'PENAL'
  AND NOT EXISTS (SELECT 1 FROM case_categories WHERE code = '2101');

INSERT INTO case_categories (code, name_ar, name_fr, case_type_id)
SELECT '2102', 'جنحي استئنافي', 'Correctionnel appel', ct.id
FROM case_types ct
WHERE ct.code = 'PENAL'
  AND NOT EXISTS (SELECT 1 FROM case_categories WHERE code = '2102');

INSERT INTO case_categories (code, name_ar, name_fr, case_type_id)
SELECT '2103', 'جنحي ضبطي', 'Contraventionnel', ct.id
FROM case_types ct
WHERE ct.code = 'PENAL'
  AND NOT EXISTS (SELECT 1 FROM case_categories WHERE code = '2103');

INSERT INTO case_categories (code, name_ar, name_fr, case_type_id)
SELECT '2104', 'جنايات', 'Criminel', ct.id
FROM case_types ct
WHERE ct.code = 'PENAL'
  AND NOT EXISTS (SELECT 1 FROM case_categories WHERE code = '2104');

INSERT INTO case_categories (code, name_ar, name_fr, case_type_id)
SELECT '2105', 'الأحداث', 'Mineurs', ct.id
FROM case_types ct
WHERE ct.code = 'PENAL'
  AND NOT EXISTS (SELECT 1 FROM case_categories WHERE code = '2105');

-- 3. Map PENAL to all statuses only if not already mapped
INSERT INTO case_type_statuses (case_type_id, status_id)
SELECT ct.id, cs.id
FROM   case_types ct
CROSS  JOIN case_statuses cs
WHERE  ct.code = 'PENAL'
  AND NOT EXISTS (
    SELECT 1 FROM case_type_statuses cts
    WHERE cts.case_type_id = ct.id
      AND cts.status_id = cs.id
  );
