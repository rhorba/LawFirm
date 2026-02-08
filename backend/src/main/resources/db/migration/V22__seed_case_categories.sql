-- Administrative Court Categories (7xxx)
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '7101', 'القضايا الاستعجالية', id FROM case_types WHERE code = 'ADM';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '7102', 'الأوامر المبنية على طلب', id FROM case_types WHERE code = 'ADM';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '7103', 'المصادقة على الحجز', id FROM case_types WHERE code = 'ADM';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '7104', 'الطعن بالإلغاء', id FROM case_types WHERE code = 'ADM';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '7105', 'القضايا الإدارية العادية', id FROM case_types WHERE code = 'ADM';

-- Commercial Court Categories (8xxx)
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '8101', 'الاستعجالي', id FROM case_types WHERE code = 'COMMERC';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '8102', 'الأمر بالأداء', id FROM case_types WHERE code = 'COMMERC';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '8103', 'صعوبات المقاولة', id FROM case_types WHERE code = 'COMMERC';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '8104', 'القضايا التجارية العادية', id FROM case_types WHERE code = 'COMMERC';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '8105', 'النزاعات التجارية', id FROM case_types WHERE code = 'COMMERC';

-- Civil Court Categories (1xxx)
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '1101', 'الاستعجالي', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '1102', 'الأوامر المبنية على طلب', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '1103', 'القضايا المدنية العادية', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '1104', 'العقود والالتزامات', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '1105', 'المسؤولية المدنية', id FROM case_types WHERE code = 'CIVIL';

-- Criminal Cases (2xxx)
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '2101', 'جنحي عادي تأديبي', id FROM case_types WHERE code = 'PENAL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '2102', 'جنحي استئنافي', id FROM case_types WHERE code = 'PENAL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '2103', 'جنحي ضبطي', id FROM case_types WHERE code = 'PENAL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '2104', 'جنايات', id FROM case_types WHERE code = 'PENAL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '2105', 'الأحداث', id FROM case_types WHERE code = 'PENAL';

-- Execution/Enforcement Cases (6xxx)
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '6101', 'البيوعات العقارية', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '6102', 'التنفيذ العقاري', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '6103', 'التنفيذ المنقول', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '6104', 'الإشكالات في التنفيذ', id FROM case_types WHERE code = 'CIVIL';

-- Court of Cassation codes (no case type link)
INSERT INTO case_categories (code, name_ar) VALUES
('1', 'رمز مدني لمحكمة النقض'),
('2', 'الغرفة الإدارية'),
('3', 'رمز تجاري لمحكمة النقض'),
('4', 'الرمز الإداري لمحكمة النقد'),
('6', 'رمز جنائي لمحكمة النقض');
