-- Assign all statuses to all case types initially (can be customized later by admin)
INSERT INTO case_type_statuses (case_type_id, status_id)
SELECT ct.id, cs.id
FROM case_types ct
CROSS JOIN case_statuses cs;
