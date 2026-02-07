package com.boilerplate.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog extends BaseEntity {

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "username")
    private String username;

    @Column(nullable = false)
    private String action;

    @Column(nullable = false)
    private String resource;

    @Column(name = "resource_id")
    private String resourceId;

    @Column(columnDefinition = "TEXT")
    private String metadata;

    @Column(name = "ip_address")
    private String ipAddress;
}
