CREATE TABLE group_roles (
    group_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (group_id, role_id),
    CONSTRAINT fk_group_roles_group FOREIGN KEY (group_id) REFERENCES groups(id) ON DELETE CASCADE,
    CONSTRAINT fk_group_roles_role FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
);

CREATE INDEX idx_group_roles_group_id ON group_roles(group_id);
CREATE INDEX idx_group_roles_role_id ON group_roles(role_id);
