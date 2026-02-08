package com.boilerplate.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "case_types")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class CaseType extends BaseEntity {

    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @Column(nullable = false, length = 100)
    private String nameFr;

    @Column(length = 100)
    private String nameAr;

    @Column(nullable = false, length = 255)
    private String numberFormatTemplate;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "case_type_statuses",
        joinColumns = @JoinColumn(name = "case_type_id"),
        inverseJoinColumns = @JoinColumn(name = "status_id")
    )
    @Builder.Default
    private Set<CaseStatus> allowedStatuses = new HashSet<>();
}
