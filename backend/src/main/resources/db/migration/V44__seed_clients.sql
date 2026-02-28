-- V44: Seed sample clients (3 individual, 3 corporate, 2 government)
INSERT INTO clients (client_type, first_name, last_name, phone, email, address, cin, gender, date_of_birth, active, created_at, updated_at)
VALUES
    ('INDIVIDUAL', 'Mohammed', 'Alaoui',  '+212661234567', 'mohammed.alaoui@email.ma',  '12 Rue Hassan II, Rabat',        'AB123456', 'MALE',   '1985-03-14', TRUE,  NOW(), NOW()),
    ('INDIVIDUAL', 'Fatima',   'Benali',  '+212662345678', 'fatima.benali@email.ma',    '45 Avenue Mohammed V, Casablanca','CD789012', 'FEMALE', '1990-07-22', TRUE,  NOW(), NOW()),
    ('INDIVIDUAL', 'Youssef',  'Chraibi', '+212663456789', 'youssef.chraibi@email.ma',  '8 Bd Zerktouni, Marrakech',      'EF345678', 'MALE',   '1978-11-05', FALSE, NOW(), NOW());

INSERT INTO clients (client_type, company_name, tax_number, first_name, phone, email, address, active, created_at, updated_at)
VALUES
    ('CORPORATE', 'Groupe Maroc Développement',  'ICE001234567890', 'Hassan',  '+212522111222', 'contact@gmd.ma',      '100 Bd Anfa, Casablanca',      TRUE,  NOW(), NOW()),
    ('CORPORATE', 'Cabinet Juridique Atlas',      'ICE002345678901', 'Karim',   '+212537222333', 'info@atlas-juridique.ma', '23 Rue Patrice Lumumba, Rabat', TRUE,  NOW(), NOW()),
    ('CORPORATE', 'SARL Tech Solutions Maroc',   'ICE003456789012', 'Samira',  '+212523333444', 'contact@techsolutions.ma','55 Zone Franche, Tanger',     FALSE, NOW(), NOW());

INSERT INTO clients (client_type, company_name, phone, email, address, active, created_at, updated_at)
VALUES
    ('GOVERNMENT', 'Ministère de la Justice',    '+212537664747', 'contact@justice.gov.ma', 'Place Mamounia, Rabat',          TRUE, NOW(), NOW()),
    ('GOVERNMENT', 'Commune Urbaine de Rabat',   '+212537723030', 'contact@commune-rabat.ma', '1 Rue Soumaya, Agdal, Rabat',  TRUE, NOW(), NOW());
