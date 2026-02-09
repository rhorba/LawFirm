-- Seed sample cases for development
-- Note: fullCaseNumber and sequenceNumber will be auto-generated, but we set them here for consistency

-- Case 1: Civil case in Rabat (2024)
INSERT INTO cases ("year", sequence_number, full_case_number, registration_date, case_description, matter_description, tribunal_id, case_type_id, case_category_id, lawyer_id, status_id, created_at, updated_at, version)
SELECT
    2024,
    1,
    'CIVIL/TR_PIN_1/2024/00001',
    '2024-03-15',
    'نزاع تجاري حول عقد بيع',
    'نزاع بين شركتين حول شروط عقد بيع معدات صناعية. المدعي يطالب بفسخ العقد ورد المبلغ المدفوع بسبب عدم مطابقة المعدات للمواصفات المتفق عليها.',
    (SELECT id FROM tribunals WHERE code = 'TR_PIN_1'),
    (SELECT id FROM case_types WHERE code = 'CIVIL'),
    (SELECT id FROM case_categories WHERE code = '1104'),
    1,
    (SELECT id FROM case_statuses WHERE code = 'IN_PROGRESS'),
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0;

-- Case 2: Commercial case in Casablanca (2024)
INSERT INTO cases ("year", sequence_number, full_case_number, registration_date, case_description, matter_description, tribunal_id, case_type_id, case_category_id, lawyer_id, status_id, created_at, updated_at, version)
SELECT
    2024,
    1,
    'COMMERC/TR_COM_PIN_2/2024/00001',
    '2024-06-20',
    'صعوبات مقاولة - طلب التسوية القضائية',
    'شركة تجارية تواجه صعوبات مالية تطلب فتح مسطرة التسوية القضائية. الديون المتراكمة تقدر بـ 2.5 مليون درهم.',
    (SELECT id FROM tribunals WHERE code = 'TR_COM_PIN_2'),
    (SELECT id FROM case_types WHERE code = 'COMMERC'),
    (SELECT id FROM case_categories WHERE code = '8103'),
    2,
    (SELECT id FROM case_statuses WHERE code = 'HEARING'),
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0;

-- Case 3: Administrative case in Rabat (2025)
INSERT INTO cases ("year", sequence_number, full_case_number, registration_date, case_description, matter_description, tribunal_id, case_type_id, case_category_id, lawyer_id, status_id, created_at, updated_at, version)
SELECT
    2025,
    1,
    'ADM/TR_ADM_PIN_1/2025/00001',
    '2025-01-10',
    'طعن بالإلغاء ضد قرار إداري',
    'طعن بالإلغاء ضد قرار إداري صادر عن الوالي يقضي بهدم بناء. الطاعن يدفع بعدم مشروعية القرار لمخالفته للإجراءات القانونية.',
    (SELECT id FROM tribunals WHERE code = 'TR_ADM_PIN_1'),
    (SELECT id FROM case_types WHERE code = 'ADM'),
    (SELECT id FROM case_categories WHERE code = '7104'),
    3,
    (SELECT id FROM case_statuses WHERE code = 'OPEN'),
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0;

-- Case 4: Criminal case in Marrakech (2025)
INSERT INTO cases ("year", sequence_number, full_case_number, registration_date, case_description, matter_description, tribunal_id, case_type_id, case_category_id, lawyer_id, status_id, created_at, updated_at, version)
SELECT
    2025,
    1,
    'PENAL/TR_PIN_37/2025/00001',
    '2025-02-05',
    'جنحة نصب واحتيال',
    'قضية نصب واحتيال. المتهم متابع بالنصب على عدة ضحايا من خلال عروض استثمارية وهمية. المبلغ الإجمالي المحتال عليه يقدر بـ 800,000 درهم.',
    (SELECT id FROM tribunals WHERE code = 'TR_PIN_37'),
    (SELECT id FROM case_types WHERE code = 'PENAL'),
    (SELECT id FROM case_categories WHERE code = '2101'),
    4,
    (SELECT id FROM case_statuses WHERE code = 'JUDGMENT'),
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0;

-- Case 5: Civil case in Casablanca (2025)
INSERT INTO cases ("year", sequence_number, full_case_number, registration_date, case_description, matter_description, tribunal_id, case_type_id, case_category_id, lawyer_id, status_id, created_at, updated_at, version)
SELECT
    2025,
    2,
    'CIVIL/TR_PIN_31/2025/00002',
    '2025-04-12',
    'دعوى تعويض عن حادثة سير',
    'دعوى مسؤولية مدنية. المدعي أصيب في حادثة سير ويطالب بتعويض عن الأضرار الجسدية والمعنوية والمادية. مبلغ التعويض المطلوب: 500,000 درهم.',
    (SELECT id FROM tribunals WHERE code = 'TR_PIN_31'),
    (SELECT id FROM case_types WHERE code = 'CIVIL'),
    (SELECT id FROM case_categories WHERE code = '1105'),
    5,
    (SELECT id FROM case_statuses WHERE code = 'IN_PROGRESS'),
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0;

-- Case 6: Commercial case in Fes (2026)
INSERT INTO cases ("year", sequence_number, full_case_number, registration_date, case_description, matter_description, tribunal_id, case_type_id, case_category_id, lawyer_id, status_id, created_at, updated_at, version)
SELECT
    2026,
    1,
    'COMMERC/TR_COM_PIN_5/2026/00001',
    '2026-01-20',
    'نزاع تجاري - أمر بالأداء',
    'طلب أمر بالأداء. الدائن يطالب بمبلغ 350,000 درهم عن فواتير غير مؤداة مقابل توريد بضائع. العقد التجاري مرفق مع جميع الوثائق الثبوتية.',
    (SELECT id FROM tribunals WHERE code = 'TR_COM_PIN_5'),
    (SELECT id FROM case_types WHERE code = 'COMMERC'),
    (SELECT id FROM case_categories WHERE code = '8102'),
    1,
    (SELECT id FROM case_statuses WHERE code = 'OPEN'),
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0;

-- Case 7: Civil urgent case in Rabat (2026)
INSERT INTO cases ("year", sequence_number, full_case_number, registration_date, case_description, matter_description, tribunal_id, case_type_id, case_category_id, lawyer_id, status_id, created_at, updated_at, version)
SELECT
    2026,
    3,
    'CIVIL/TR_PIN_1/2026/00003',
    '2026-02-01',
    'دعوى استعجالية - منع معارضة',
    'طلب استعجالي لمنع معارضة في أشغال بناء. المدعي يملك رخصة البناء القانونية والجار يعارض في الأشغال دون سند قانوني.',
    (SELECT id FROM tribunals WHERE code = 'TR_PIN_1'),
    (SELECT id FROM case_types WHERE code = 'CIVIL'),
    (SELECT id FROM case_categories WHERE code = '1101'),
    2,
    (SELECT id FROM case_statuses WHERE code = 'HEARING'),
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0;

-- Case 8: Draft case (not yet submitted)
INSERT INTO cases ("year", sequence_number, full_case_number, registration_date, case_description, matter_description, tribunal_id, case_type_id, case_category_id, lawyer_id, status_id, created_at, updated_at, version)
SELECT
    2026,
    4,
    'CIVIL/TR_PIN_1/2026/00004',
    '2026-02-08',
    'دعوى فسخ عقد كراء',
    'طلب فسخ عقد كراء تجاري بسبب عدم أداء المكتري للوجيبة الكرائية لمدة 6 أشهر. المبلغ المطلوب: 180,000 درهم.',
    (SELECT id FROM tribunals WHERE code = 'TR_PIN_1'),
    (SELECT id FROM case_types WHERE code = 'CIVIL'),
    (SELECT id FROM case_categories WHERE code = '1104'),
    3,
    (SELECT id FROM case_statuses WHERE code = 'DRAFT'),
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0;

-- Case 9: Closed case
INSERT INTO cases ("year", sequence_number, full_case_number, registration_date, case_description, matter_description, tribunal_id, case_type_id, case_category_id, lawyer_id, status_id, created_at, updated_at, version)
SELECT
    2023,
    1,
    'ADM/TR_ADM_PIN_2/2023/00001',
    '2023-08-15',
    'قضية إدارية عادية',
    'نزاع إداري حول قرار توظيف. تم البت في القضية لصالح المدعي مع إلغاء القرار الإداري المطعون فيه.',
    (SELECT id FROM tribunals WHERE code = 'TR_ADM_PIN_2'),
    (SELECT id FROM case_types WHERE code = 'ADM'),
    (SELECT id FROM case_categories WHERE code = '7105'),
    4,
    (SELECT id FROM case_statuses WHERE code = 'CLOSED'),
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0;

-- Case 10: Another commercial case in Tanger (2026)
INSERT INTO cases ("year", sequence_number, full_case_number, registration_date, case_description, matter_description, tribunal_id, case_type_id, case_category_id, lawyer_id, status_id, created_at, updated_at, version)
SELECT
    2026,
    1,
    'COMMERC/TR_COM_PIN_8/2026/00001',
    '2026-01-28',
    'نزاع حول علامة تجارية',
    'نزاع تجاري حول استعمال علامة تجارية مشابهة. المدعي صاحب العلامة المسجلة يطالب بوقف الاستعمال غير المشروع والتعويض عن الضرر.',
    (SELECT id FROM tribunals WHERE code = 'TR_COM_PIN_8'),
    (SELECT id FROM case_types WHERE code = 'COMMERC'),
    (SELECT id FROM case_categories WHERE code = '8105'),
    5,
    (SELECT id FROM case_statuses WHERE code = 'IN_PROGRESS'),
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0;
