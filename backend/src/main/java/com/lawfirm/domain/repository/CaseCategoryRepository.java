package com.lawfirm.domain.repository;

import com.lawfirm.domain.model.CaseCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CaseCategoryRepository extends JpaRepository<CaseCategory, Long> {

    Optional<CaseCategory> findByCode(String code);

    Optional<CaseCategory> findByCodeAndActiveTrue(String code);

    List<CaseCategory> findAllByActiveTrueOrderByCodeAsc();

    List<CaseCategory> findByCaseTypeIdAndActiveTrue(Long caseTypeId);
}
