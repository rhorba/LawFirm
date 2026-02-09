package com.lawfirm.domain.repository;

import com.lawfirm.domain.model.CaseStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CaseStatusRepository extends JpaRepository<CaseStatus, Long> {

    Optional<CaseStatus> findByCode(String code);

    List<CaseStatus> findAllByOrderBySortOrderAsc();
}
