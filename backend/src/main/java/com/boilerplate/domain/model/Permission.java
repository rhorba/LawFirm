package com.boilerplate.domain.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "permissions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@lombok.experimental.SuperBuilder
public class Permission extends BaseEntity {

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(length = 255)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private PermissionResource resource;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private PermissionAction action;

    public enum PermissionResource {
        USER, ROLE, PERMISSION, SYSTEM
    }

    public enum PermissionAction {
        READ, CREATE, UPDATE, DELETE, MANAGE
    }
}
