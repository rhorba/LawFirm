-- V37: Create case_lawyers join table and case_templates table
-- Must run before V38 which migrates data into case_lawyers

-- Many-to-many: lawyers assigned to a case
CREATE TABLE case_lawyers (
    case_id   BIGINT NOT NULL,
    lawyer_id BIGINT NOT NULL,
    PRIMARY KEY (case_id, lawyer_id),
    CONSTRAINT fk_case_lawyers_case   FOREIGN KEY (case_id)   REFERENCES cases(id)   ON DELETE CASCADE,
    CONSTRAINT fk_case_lawyers_lawyer FOREIGN KEY (lawyer_id) REFERENCES lawyers(id)
);

CREATE INDEX idx_case_lawyers_lawyer ON case_lawyers(lawyer_id);

-- Case templates: store type + category presets for quick case creation
CREATE TABLE case_templates (
    id                 BIGSERIAL    PRIMARY KEY,
    name               VARCHAR(100) NOT NULL UNIQUE,
    case_type_code     VARCHAR(20)  NOT NULL,
    case_category_code VARCHAR(20)  NOT NULL,
    created_at         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version            BIGINT       NOT NULL DEFAULT 0
);
