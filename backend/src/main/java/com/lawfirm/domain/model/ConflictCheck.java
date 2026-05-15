package com.lawfirm.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "conflict_checks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class ConflictCheck extends BaseEntity {

    @Column(name = "search_name", nullable = false, length = 255)
    private String searchName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CheckResult result;

    @Column(name = "cleared_note", columnDefinition = "TEXT")
    private String clearedNote;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "performed_by_user_id", nullable = false)
    private User performedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cleared_by_user_id")
    private User clearedBy;

    public enum CheckResult {
        CLEAR, CONFLICT_FOUND
    }
}
