CREATE TABLE task_comments (
    id               BIGSERIAL   PRIMARY KEY,
    task_id          BIGINT      NOT NULL,
    author_user_id   BIGINT      NOT NULL,
    content          TEXT        NOT NULL,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_task_comments_task FOREIGN KEY (task_id)        REFERENCES tasks(id) ON DELETE CASCADE,
    CONSTRAINT fk_task_comments_user FOREIGN KEY (author_user_id) REFERENCES users(id)
);

CREATE INDEX idx_task_comments_task_id ON task_comments(task_id);
