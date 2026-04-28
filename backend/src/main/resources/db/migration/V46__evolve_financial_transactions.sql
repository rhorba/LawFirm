-- V46__evolve_financial_transactions.sql
ALTER TABLE financial_transactions ADD COLUMN direction VARCHAR(10) NOT NULL DEFAULT 'EXPENSE';
ALTER TABLE financial_transactions ADD COLUMN operation_type VARCHAR(20) NOT NULL DEFAULT 'OTHER';
ALTER TABLE financial_transactions ADD COLUMN payment_mode VARCHAR(15);
ALTER TABLE financial_transactions ADD COLUMN account_number VARCHAR(50);
ALTER TABLE financial_transactions ADD COLUMN deleted_at TIMESTAMP;

ALTER TABLE financial_transactions
  ADD CONSTRAINT chk_ft_direction CHECK (direction IN ('REVENUE', 'EXPENSE'));

ALTER TABLE financial_transactions
  ADD CONSTRAINT chk_ft_operation_type CHECK (operation_type IN (
    'OPENING_FEE', 'PROCEDURE_FEE', 'INTERVENTION_FEE', 'EXPERT_FEE',
    'DOCUMENT_FEE', 'NOTIFICATION_FEE', 'JUDICIAL_TAX', 'OTHER'));

ALTER TABLE financial_transactions
  ADD CONSTRAINT chk_ft_payment_mode CHECK (payment_mode IN (
    'CHECK', 'TRANSFER', 'CASH', 'CREDIT_CARD', 'MONEY_ORDER'));

-- Migrate existing data
UPDATE financial_transactions
  SET direction =
    CASE WHEN transaction_type = 'PAYMENT' THEN 'REVENUE' ELSE 'EXPENSE' END;

-- Drop old column (also removes its CHECK constraint)
ALTER TABLE financial_transactions DROP COLUMN transaction_type;

CREATE INDEX idx_transactions_direction  ON financial_transactions(direction);
CREATE INDEX idx_transactions_deleted_at ON financial_transactions(deleted_at);
