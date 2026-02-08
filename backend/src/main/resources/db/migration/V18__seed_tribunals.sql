-- Administrative Appeal Courts
INSERT INTO tribunals (code, name_fr, name_ar) VALUES
('TR_ADM_APPL_1', 'Tribunal d''appel administratif de Rabat', 'محكمة الاستئناف الإدارية بالرباط'),
('TR_ADM_APPL_2', 'Tribunal d''appel administratif de Marrakech', 'محكمة الإستئناف الإدارية بمراكش');

-- Administrative First Instance Courts
INSERT INTO tribunals (code, name_fr, name_ar) VALUES
('TR_ADM_PIN_1', 'Tribunal administratif de Rabat', 'المحكمة الإدارية بالرباط'),
('TR_ADM_PIN_2', 'Tribunal administratif de Casablanca', 'المحكمة الإدارية بالدار البيضاء'),
('TR_ADM_PIN_3', 'Tribunal administratif de Fes', 'المحكمة الإدارية بفاس'),
('TR_ADM_PIN_4', 'Tribunal administratif de Meknes', 'المحكمة الإدارية بمكناس'),
('TR_ADM_PIN_5', 'Tribunal administratif de Oujda', 'المحكمة الإدارية بوجدة'),
('TR_ADM_PIN_6', 'Tribunal administratif de Marrakech', 'المحكمة الإدارية بمراكش'),
('TR_ADM_PIN_7', 'Tribunal administratif de Agadir', 'المحكمة الإدارية بأكادير');

-- Commercial Appeal Courts
INSERT INTO tribunals (code, name_fr, name_ar) VALUES
('TR_COM_APPL_1', 'Tribunal d''appel commercial de Casablanca', 'محكمة الاستئناف التجارية - الدار البيضاء'),
('TR_COM_APPL_2', 'Tribunal d''appel commercial de Marrakech', 'محكمة الاستئناف التجارية - مراكش'),
('TR_COM_APPL_3', 'Tribunal d''appel commercial de Fes', 'محكمة الاستئناف التجارية - فاس');

-- Commercial First Instance Courts
INSERT INTO tribunals (code, name_fr, name_ar) VALUES
('TR_COM_PIN_1', 'Tribunal commercial de Rabat', 'المحكمة التجارية - الرباط'),
('TR_COM_PIN_2', 'Tribunal commercial de Casablanca', 'المحكمة التجارية - الدار البيضاء'),
('TR_COM_PIN_3', 'Tribunal commercial de Agadir', 'المحكمة التجارية - أكادير'),
('TR_COM_PIN_4', 'Tribunal commercial de Marrakech', 'المحكمة التجارية مراكش'),
('TR_COM_PIN_5', 'Tribunal commercial de Fes', 'المحكمة التجارية بفاس'),
('TR_COM_PIN_6', 'Tribunal commercial de Meknes', 'المحكمة التجارية بمكناس'),
('TR_COM_PIN_7', 'Tribunal commercial de Oujda', 'المحكمة التجارية بوجدة'),
('TR_COM_PIN_8', 'Tribunal commercial de Tanger', 'المحكمة التجارية بطنجة');

-- Appeal Courts (22 courts)
INSERT INTO tribunals (code, name_fr, name_ar) VALUES
('TR_APPL_1', 'Tribunal d''appel de Rabat', 'محكمة الاستئناف - بالرباط'),
('TR_APPL_2', 'Tribunal d''appel de Laayoune', 'محكمة الاستئناف العيون'),
('TR_APPL_3', 'Tribunal d''appel de Agadir', 'محكمة الاستئناف أكادير'),
('TR_APPL_4', 'Tribunal d''appel de Ouarzazate', 'محكمة الاستئناف ورزازات'),
('TR_APPL_5', 'Tribunal d''appel de Kenitra', 'محكمة الاستئناف القنيطرة'),
('TR_APPL_6', 'Tribunal d''appel de Settat', 'محكمة الاستئناف سطات'),
('TR_APPL_7', 'Tribunal d''appel de Khouribga', 'محكمة الاستئناف - خريبكة'),
('TR_APPL_8', 'Tribunal d''appel de Casablanca', 'محكمة الاستئناف - الدر البيضاء'),
('TR_APPL_9', 'Tribunal d''appel de Marrakech', 'محكمة الاستئناف - مراكش'),
('TR_APPL_10', 'Tribunal d''appel de Safi', 'محكمة الاستئناف - آسفي'),
('TR_APPL_11', 'Tribunal d''appel de Tanger', 'محكمة الاستئناف - طنجة'),
('TR_APPL_12', 'Tribunal d''appel de Tetouan', 'محكمة الاستئناف - تطوان'),
('TR_APPL_13', 'Tribunal d''appel de Al Hoceima', 'محكمة الاستئناف - الحسيمة'),
('TR_APPL_14', 'Tribunal d''appel de Taza', 'محكمة الاستئناف - تازة'),
('TR_APPL_15', 'Tribunal d''appel de Oujda', 'محكمة الاستئناف - وجدة'),
('TR_APPL_16', 'Tribunal d''appel de Meknes', 'محكمة الاستئناف - مكناس'),
('TR_APPL_17', 'Tribunal d''appel de El Jadida', 'محكمة الاستئناف - الجديدة'),
('TR_APPL_18', 'Tribunal d''appel de Beni Mellal', 'محكمة الاستئناف - بني ملال'),
('TR_APPL_19', 'Tribunal d''appel de Errachidia', 'محكمة الاستئناف - الرشيدية'),
('TR_APPL_20', 'Tribunal d''appel de Nador', 'محكمة الاستئناف - الناظور'),
('TR_APPL_21', 'Tribunal d''appel de Fes', 'محكمة الاستئناف - فاس'),
('TR_APPL_22', 'Tribunal d''appel de Guelmim', 'محكمة الاستئناف - كلميم');

-- First Instance Courts (sample - 10 major courts, expandable to all 83)
INSERT INTO tribunals (code, name_fr, name_ar) VALUES
('TR_PIN_1', 'Tribunal de 1ère instance de Rabat', 'المحكمة الابتدائية - الرباط'),
('TR_PIN_2', 'Tribunal de 1ère instance de Salé', 'المحكمة الابتدائية سلا'),
('TR_PIN_3', 'Tribunal de 1ère instance de Temara', 'المحكمة الابتدائية - تمارة'),
('TR_PIN_4', 'Tribunal de 1ère instance de Khemisset', 'المحكمة االابتدائية - الخميسات'),
('TR_PIN_5', 'Tribunal de 1ère instance de Rommani', 'المحكمة االابتدائية - الرماني'),
('TR_PIN_6', 'Tribunal de 1ère instance de Tiflet', 'المحكمة الابتدائية - تيفلت'),
('TR_PIN_31', 'Tribunal de 1ère instance civile de Casablanca', 'المحكمة الابتدائية المدنية بالدار البيضاء'),
('TR_PIN_37', 'Tribunal de 1ère instance de Marrakech', 'المحكمة الابتدائية - مراكش'),
('TR_PIN_44', 'Tribunal de 1ère instance de Tanger', 'المحكمة الابتدائية - طنجة'),
('TR_PIN_76', 'Tribunal de 1ère instance de Fes', 'المحكمة الابتدائية - فاس');

-- Court of Cassation
INSERT INTO tribunals (code, name_fr, name_ar) VALUES
('TR_CASS_1', 'Cour de cassation de Rabat', 'محكمة النقض بالرباط');
