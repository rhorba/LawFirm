package com.boilerplate.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "case_categories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class CaseCategory extends BaseEntity {

    @Column(nullable = false, unique = true, length = 10)
    private String code;

    @Column(nullable = false, length = 255)
    private String nameAr;

    @Column(length = 255)
    private String nameFr;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_type_id")
    private CaseType caseType;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;
}
