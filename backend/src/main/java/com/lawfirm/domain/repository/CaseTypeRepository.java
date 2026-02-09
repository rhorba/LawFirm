package com.lawfirm.domain.repository;

import com.lawfirm.domain.model.CaseType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CaseTypeRepository extends JpaRepository<CaseType, Long> {

    Optional<CaseType> findByCode(String code);

    Optional<CaseType> findByCodeAndActiveTrue(String code);

    @Query("SELECT ct FROM CaseType ct LEFT JOIN FETCH ct.allowedStatuses WHERE ct.code = :code")
    Optional<CaseType> findByCodeWithStatuses(@Param("code") String code);

    List<CaseType> findAllByActiveTrue();
}
