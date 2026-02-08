INSERT INTO case_statuses (code, name_fr, name_ar, sort_order, is_terminal) VALUES
('DRAFT', 'Brouillon', 'مسودة', 1, false),
('OPEN', 'Ouvert', 'مفتوح', 2, false),
('IN_PROGRESS', 'En cours', 'قيد التقدم', 3, false),
('HEARING', 'Audience', 'جلسة', 4, false),
('JUDGMENT', 'Jugement', 'حكم', 5, false),
('CLOSED', 'Clôturé', 'مغلق', 6, true),
('ARCHIVED', 'Archivé', 'مؤرشف', 7, true);
