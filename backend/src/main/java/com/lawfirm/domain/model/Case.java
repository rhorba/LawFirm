package com.lawfirm.domain.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "cases")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Case extends BaseEntity {

    @Column(name = "\"year\"", nullable = false)
    private Integer year;

    @Column(nullable = false)
    private Integer sequenceNumber;

    @Column(nullable = false, unique = true, length = 255)
    private String fullCaseNumber;

    @Column(nullable = false)
    private LocalDate registrationDate;

    @Column(nullable = false, length = 500)
    private String caseDescription;

    @Column(columnDefinition = "TEXT")
    private String matterDescription;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tribunal_id", nullable = false)
    private Tribunal tribunal;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_type_id", nullable = false)
    private CaseType caseType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_category_id")
    private CaseCategory caseCategory;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lawyer_id", nullable = false)
    private Lawyer lawyer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "status_id", nullable = false)
    private CaseStatus status;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @OneToMany(mappedBy = "caseEntity", cascade = CascadeType.ALL)
    @Builder.Default
    private List<FinancialTransaction> transactions = new ArrayList<>();
}
