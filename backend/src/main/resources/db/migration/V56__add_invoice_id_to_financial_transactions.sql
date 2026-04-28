ALTER TABLE financial_transactions ADD COLUMN invoice_id BIGINT;

ALTER TABLE financial_transactions
    ADD CONSTRAINT fk_financial_transactions_invoice
        FOREIGN KEY (invoice_id) REFERENCES invoices(id) ON DELETE SET NULL;

ALTER TABLE financial_transactions
    ADD CONSTRAINT uq_financial_transactions_invoice
        UNIQUE (invoice_id);
