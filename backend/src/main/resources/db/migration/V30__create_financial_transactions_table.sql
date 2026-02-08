CREATE TABLE financial_transactions (
    id BIGSERIAL PRIMARY KEY,
    case_id BIGINT NOT NULL,
    transaction_type VARCHAR(20) NOT NULL CHECK (transaction_type IN ('PAYMENT', 'EXPENSE')),
    amount DECIMAL(15,2) NOT NULL,
    payment_date DATE,
    payment_reference VARCHAR(100),
    lawyer_payment_year INT,
    fiscal_year_from DATE,
    fiscal_year_to DATE,
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    FOREIGN KEY (case_id) REFERENCES cases(id) ON DELETE CASCADE
);

CREATE INDEX idx_transactions_case ON financial_transactions(case_id);
CREATE INDEX idx_transactions_payment_date ON financial_transactions(payment_date);
CREATE INDEX idx_transactions_type ON financial_transactions(transaction_type);
CREATE INDEX idx_transactions_lawyer_payment_year ON financial_transactions(lawyer_payment_year);
