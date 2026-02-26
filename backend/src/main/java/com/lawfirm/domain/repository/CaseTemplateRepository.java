package com.lawfirm.domain.repository;

import com.lawfirm.domain.model.CaseTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CaseTemplateRepository extends JpaRepository<CaseTemplate, Long> {
    boolean existsByName(String name);
}
