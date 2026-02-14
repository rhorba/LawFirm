-- Seed sample lawyers for development
INSERT INTO lawyers (first_name, last_name, tax_id, email, phone, active, created_at, updated_at, version)
VALUES
    ('Ahmed', 'BENOMAR', 'TAX001', 'ahmed.benomar@lawfirm.ma', '+212-6-12-34-56-78', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    ('Fatima', 'ALAOUI', 'TAX002', 'fatima.alaoui@lawfirm.ma', '+212-6-23-45-67-89', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    ('Youssef', 'IDRISSI', 'TAX003', 'youssef.idrissi@lawfirm.ma', '+212-6-34-56-78-90', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    ('Samira', 'KETTANI', 'TAX004', 'samira.kettani@lawfirm.ma', '+212-6-45-67-89-01', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    ('Karim', 'BENJELLOUN', 'TAX005', 'karim.benjelloun@lawfirm.ma', '+212-6-56-78-90-12', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    ('Nadia', 'LAZRAK', 'TAX006', 'nadia.lazrak@lawfirm.ma', '+212-6-67-89-01-23', false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);
