-- V54: Create group_permissions junction table for direct permission assignment to groups

CREATE TABLE group_permissions (
    group_id    BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    PRIMARY KEY (group_id, permission_id),
    CONSTRAINT fk_gp_group      FOREIGN KEY (group_id)      REFERENCES groups(id)      ON DELETE CASCADE,
    CONSTRAINT fk_gp_permission FOREIGN KEY (permission_id) REFERENCES permissions(id) ON DELETE CASCADE
);

CREATE INDEX idx_group_permissions_group_id      ON group_permissions(group_id);
CREATE INDEX idx_group_permissions_permission_id ON group_permissions(permission_id);
