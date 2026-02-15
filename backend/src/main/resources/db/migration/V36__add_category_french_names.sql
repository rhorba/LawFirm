-- Add French names to case categories

-- Administrative Court Categories
UPDATE case_categories SET name_fr = 'Référé administratif' WHERE code = '7101';
UPDATE case_categories SET name_fr = 'Ordonnance sur requête' WHERE code = '7102';
UPDATE case_categories SET name_fr = 'Validation de saisie' WHERE code = '7103';
UPDATE case_categories SET name_fr = 'Recours en annulation' WHERE code = '7104';
UPDATE case_categories SET name_fr = 'Affaires administratives ordinaires' WHERE code = '7105';

-- Commercial Court Categories
UPDATE case_categories SET name_fr = 'Référé commercial' WHERE code = '8101';
UPDATE case_categories SET name_fr = 'Injonction de payer' WHERE code = '8102';
UPDATE case_categories SET name_fr = 'Difficultés d''entreprise' WHERE code = '8103';
UPDATE case_categories SET name_fr = 'Affaires commerciales ordinaires' WHERE code = '8104';
UPDATE case_categories SET name_fr = 'Litiges commerciaux' WHERE code = '8105';

-- Civil Court Categories
UPDATE case_categories SET name_fr = 'Référé civil' WHERE code = '1101';
UPDATE case_categories SET name_fr = 'Ordonnance sur requête' WHERE code = '1102';
UPDATE case_categories SET name_fr = 'Affaires civiles ordinaires' WHERE code = '1103';
UPDATE case_categories SET name_fr = 'Contrats et obligations' WHERE code = '1104';
UPDATE case_categories SET name_fr = 'Responsabilité civile' WHERE code = '1105';

-- Criminal Categories (PENAL)
UPDATE case_categories SET name_fr = 'Correctionnel ordinaire' WHERE code = '2101';
UPDATE case_categories SET name_fr = 'Correctionnel en appel' WHERE code = '2102';
UPDATE case_categories SET name_fr = 'Correctionnel de police' WHERE code = '2103';
UPDATE case_categories SET name_fr = 'Crimes' WHERE code = '2104';
UPDATE case_categories SET name_fr = 'Affaires des mineurs' WHERE code = '2105';

-- Execution/Enforcement Categories
UPDATE case_categories SET name_fr = 'Ventes immobilières' WHERE code = '6101';
UPDATE case_categories SET name_fr = 'Exécution immobilière' WHERE code = '6102';
UPDATE case_categories SET name_fr = 'Exécution mobilière' WHERE code = '6103';
UPDATE case_categories SET name_fr = 'Incidents d''exécution' WHERE code = '6104';

-- Court of Cassation categories
UPDATE case_categories SET name_fr = 'Civil - Cour de cassation' WHERE code = '1';
UPDATE case_categories SET name_fr = 'Chambre administrative' WHERE code = '2';
UPDATE case_categories SET name_fr = 'Commercial - Cour de cassation' WHERE code = '3';
UPDATE case_categories SET name_fr = 'Administratif - Cour de cassation' WHERE code = '4';
UPDATE case_categories SET name_fr = 'Pénal - Cour de cassation' WHERE code = '6';
