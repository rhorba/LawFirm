CREATE TABLE documents (
    id                   BIGSERIAL      PRIMARY KEY,
    case_id              BIGINT         NOT NULL,
    title                VARCHAR(255)   NOT NULL,
    description          TEXT,
    category             VARCHAR(30)    NOT NULL DEFAULT 'AUTRE',
    original_filename    VARCHAR(255)   NOT NULL,
    stored_filename      VARCHAR(255)   NOT NULL,
    content_type         VARCHAR(100)   NOT NULL,
    file_size            BIGINT         NOT NULL,
    uploaded_by_user_id  BIGINT         NOT NULL,
    version              BIGINT         NOT NULL DEFAULT 0,
    created_at           TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_document_case    FOREIGN KEY (case_id)             REFERENCES cases(id),
    CONSTRAINT fk_document_user    FOREIGN KEY (uploaded_by_user_id) REFERENCES users(id),
    CONSTRAINT uq_document_stored  UNIQUE (stored_filename),
    CONSTRAINT chk_document_category CHECK (category IN ('CONTRAT','JUGEMENT','ASSIGNATION','COURRIER','REQUETE','AUTRE'))
);

CREATE INDEX idx_documents_case_id  ON documents(case_id);
CREATE INDEX idx_documents_category ON documents(category);
