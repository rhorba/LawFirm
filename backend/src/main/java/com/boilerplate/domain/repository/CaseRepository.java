package com.boilerplate.domain.repository;

import com.boilerplate.domain.model.Case;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CaseRepository extends JpaRepository<Case, Long>, JpaSpecificationExecutor<Case> {

    Optional<Case> findByFullCaseNumber(String fullCaseNumber);

    @Query("SELECT c FROM Case c " +
           "LEFT JOIN FETCH c.tribunal " +
           "LEFT JOIN FETCH c.caseType " +
           "LEFT JOIN FETCH c.caseCategory " +
           "LEFT JOIN FETCH c.lawyer " +
           "LEFT JOIN FETCH c.status " +
           "WHERE c.id = :id")
    Optional<Case> findByIdWithDetails(@Param("id") Long id);

    boolean existsByFullCaseNumber(String fullCaseNumber);
}
