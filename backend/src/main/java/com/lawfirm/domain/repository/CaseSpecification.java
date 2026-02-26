package com.lawfirm.domain.repository;

import com.lawfirm.domain.model.Case;
import com.lawfirm.domain.model.CasePriority;
import com.lawfirm.domain.model.Lawyer;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class CaseSpecification {

    public static Specification<Case> withFilters(
        Integer year,
        String caseTypeCode,
        String categoryCode,
        String tribunalCode,
        Long lawyerId,
        String statusCode,
        String priority,
        LocalDate registrationDateFrom,
        LocalDate registrationDateTo,
        String search
    ) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Not deleted
            predicates.add(criteriaBuilder.isNull(root.get("deletedAt")));

            if (year != null) {
                predicates.add(criteriaBuilder.equal(root.get("year"), year));
            }

            if (caseTypeCode != null && !caseTypeCode.isBlank()) {
                predicates.add(criteriaBuilder.equal(root.get("caseType").get("code"), caseTypeCode));
            }

            if (categoryCode != null && !categoryCode.isBlank()) {
                predicates.add(criteriaBuilder.equal(root.get("caseCategory").get("code"), categoryCode));
            }

            if (tribunalCode != null && !tribunalCode.isBlank()) {
                predicates.add(criteriaBuilder.equal(root.get("tribunal").get("code"), tribunalCode));
            }

            if (lawyerId != null) {
                Join<Case, Lawyer> lawyerJoin = root.join("lawyers", JoinType.INNER);
                predicates.add(criteriaBuilder.equal(lawyerJoin.get("id"), lawyerId));
                query.distinct(true);
            }

            if (statusCode != null && !statusCode.isBlank()) {
                predicates.add(criteriaBuilder.equal(root.get("status").get("code"), statusCode));
            }

            if (priority != null && !priority.isBlank()) {
                try {
                    CasePriority priorityEnum = CasePriority.valueOf(priority.toUpperCase());
                    predicates.add(criteriaBuilder.equal(root.get("priority"), priorityEnum));
                } catch (IllegalArgumentException ignored) {
                    // unknown priority value — ignore filter
                }
            }

            if (registrationDateFrom != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                    root.get("registrationDate"), registrationDateFrom));
            }

            if (registrationDateTo != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(
                    root.get("registrationDate"), registrationDateTo));
            }

            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.toLowerCase() + "%";
                predicates.add(criteriaBuilder.or(
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("fullCaseNumber")), pattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("caseDescription")), pattern)
                ));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
