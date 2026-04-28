-- V62__wire_case_type_statuses_for_adm.sql
-- V26 already assigns all statuses to all case types via CROSS JOIN.
-- This migration is a safety net in case the ADM type was added after V26.
-- ON CONFLICT DO NOTHING makes it idempotent.

INSERT INTO case_type_statuses (case_type_id, status_id)
SELECT ct.id, cs.id
FROM case_types ct
CROSS JOIN case_statuses cs
WHERE ct.code = 'ADM'
AND NOT EXISTS (
    SELECT 1 FROM case_type_statuses cts
    WHERE cts.case_type_id = ct.id AND cts.status_id = cs.id
);
