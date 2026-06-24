package com.lawfirm.domain.repository;

import com.lawfirm.domain.model.Case;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CaseRepository extends JpaRepository<Case, Long>, JpaSpecificationExecutor<Case> {

    Optional<Case> findByFullCaseNumber(String fullCaseNumber);

    @Query("SELECT c FROM Case c " +
           "LEFT JOIN FETCH c.tribunal " +
           "LEFT JOIN FETCH c.caseType ct " +
           "LEFT JOIN FETCH ct.allowedStatuses " +
           "LEFT JOIN FETCH c.caseCategory " +
           "LEFT JOIN FETCH c.lawyers " +
           "LEFT JOIN FETCH c.status " +
           "LEFT JOIN FETCH c.parentCase " +
           "WHERE c.id = :id AND c.deletedAt IS NULL")
    Optional<Case> findByIdWithDetails(@Param("id") Long id);

    boolean existsByFullCaseNumber(String fullCaseNumber);

    List<Case> findByParentCaseIdAndDeletedAtIsNull(Long parentCaseId);

    @Query("SELECT c FROM Case c WHERE c.deletedAt IS NULL AND " +
           "(LOWER(c.fullCaseNumber) LIKE LOWER(CONCAT('%',:name,'%')) OR " +
           " LOWER(c.caseDescription) LIKE LOWER(CONCAT('%',:name,'%')))")
    List<Case> searchByName(@Param("name") String name);
}
