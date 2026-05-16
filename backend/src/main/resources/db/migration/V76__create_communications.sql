CREATE TABLE communications (
    id                  BIGSERIAL       PRIMARY KEY,
    case_id             BIGINT          NOT NULL,
    comm_type           VARCHAR(20)     NOT NULL,
    direction           VARCHAR(10)     NOT NULL,
    subject             VARCHAR(255),
    body                TEXT,
    recipient_email     VARCHAR(255),
    recipient_phone     VARCHAR(50),
    sent_by_user_id     BIGINT          NOT NULL,
    version             BIGINT          NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_communication_case FOREIGN KEY (case_id)          REFERENCES cases(id),
    CONSTRAINT fk_communication_user FOREIGN KEY (sent_by_user_id)  REFERENCES users(id),
    CONSTRAINT chk_comm_type      CHECK (comm_type IN ('NOTE','EMAIL_SENT','EMAIL_RECEIVED','CALL','SMS')),
    CONSTRAINT chk_comm_direction CHECK (direction IN ('INBOUND','OUTBOUND','INTERNAL'))
);

CREATE INDEX idx_communications_case_id ON communications(case_id);
CREATE INDEX idx_communications_type    ON communications(comm_type);
