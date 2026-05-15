CREATE TABLE calendar_events (
    id                  BIGSERIAL     PRIMARY KEY,
    title               VARCHAR(255)  NOT NULL,
    description         TEXT,
    event_type          VARCHAR(20)   NOT NULL,
    start_datetime      TIMESTAMPTZ   NOT NULL,
    end_datetime        TIMESTAMPTZ,
    all_day             BOOLEAN       NOT NULL DEFAULT FALSE,
    case_id             BIGINT,
    created_by_user_id  BIGINT        NOT NULL,
    version             BIGINT        NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ   NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_cal_events_case FOREIGN KEY (case_id)           REFERENCES cases(id),
    CONSTRAINT fk_cal_events_user FOREIGN KEY (created_by_user_id) REFERENCES users(id),
    CONSTRAINT chk_cal_event_type CHECK (event_type IN ('HEARING','APPOINTMENT','REMINDER'))
);

CREATE INDEX idx_cal_events_start          ON calendar_events(start_datetime);
CREATE INDEX idx_cal_events_case_id        ON calendar_events(case_id);
CREATE INDEX idx_cal_events_created_by     ON calendar_events(created_by_user_id);
