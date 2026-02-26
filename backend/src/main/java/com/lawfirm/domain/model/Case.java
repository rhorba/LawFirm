package com.lawfirm.domain.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "case_lawyers",
        joinColumns = @JoinColumn(name = "case_id"),
        inverseJoinColumns = @JoinColumn(name = "lawyer_id")
    )
    @Builder.Default
    private Set<Lawyer> lawyers = new HashSet<>();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "status_id", nullable = false)
    private CaseStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private CasePriority priority = CasePriority.NORMAL;

    @Column(length = 255)
    private String opposingParty;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private CaseOutcome outcome;

    @Column(columnDefinition = "TEXT")
    private String outcomeNotes;

    @Column(name = "initial_payment_date")
    private LocalDate initialPaymentDate;

    @Column(name = "fiscal_year")
    private Short fiscalYear;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_case_id")
    private Case parentCase;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @OneToMany(mappedBy = "caseEntity", cascade = CascadeType.ALL)
    @Builder.Default
    private List<FinancialTransaction> transactions = new ArrayList<>();
}
