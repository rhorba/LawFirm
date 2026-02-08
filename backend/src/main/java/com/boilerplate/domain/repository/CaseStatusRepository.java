package com.boilerplate.domain.repository;

import com.boilerplate.domain.model.CaseStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CaseStatusRepository extends JpaRepository<CaseStatus, Long> {

    Optional<CaseStatus> findByCode(String code);

    List<CaseStatus> findAllByOrderBySortOrderAsc();
}
