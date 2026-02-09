package com.lawfirm.domain.repository;

import com.lawfirm.domain.model.CaseSequence;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CaseSequenceRepository extends JpaRepository<CaseSequence, Long> {

    Optional<CaseSequence> findByYearAndCaseTypeCode(Integer year, String caseTypeCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT cs FROM CaseSequence cs WHERE cs.year = :year AND cs.caseTypeCode = :caseTypeCode")
    Optional<CaseSequence> findByYearAndCaseTypeCodeForUpdate(
            @Param("year") Integer year,
            @Param("caseTypeCode") String caseTypeCode
    );
}
