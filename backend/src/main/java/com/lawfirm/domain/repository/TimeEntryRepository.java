package com.lawfirm.domain.repository;

import com.lawfirm.domain.model.TimeEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface TimeEntryRepository extends JpaRepository<TimeEntry, Long> {

    @Query("SELECT t FROM TimeEntry t " +
           "LEFT JOIN FETCH t.lawyer " +
           "LEFT JOIN FETCH t.invoice " +
           "WHERE t.caseEntity.id = :caseId " +
           "ORDER BY t.entryDate DESC, t.createdAt DESC")
    List<TimeEntry> findByCaseId(@Param("caseId") Long caseId);

    @Query("SELECT t FROM TimeEntry t " +
           "LEFT JOIN FETCH t.caseEntity " +
           "LEFT JOIN FETCH t.lawyer " +
           "WHERE t.id = :id")
    Optional<TimeEntry> findByIdWithDetails(@Param("id") Long id);

    @Query("SELECT COALESCE(SUM(t.hours), 0) FROM TimeEntry t WHERE t.caseEntity.id = :caseId")
    BigDecimal sumHoursByCaseId(@Param("caseId") Long caseId);

    @Query("SELECT COALESCE(SUM(t.hours), 0) FROM TimeEntry t WHERE t.caseEntity.id = :caseId AND t.billable = true")
    BigDecimal sumBillableHoursByCaseId(@Param("caseId") Long caseId);

    @Query("SELECT COALESCE(SUM(t.hours), 0) FROM TimeEntry t WHERE t.caseEntity.id = :caseId AND t.billed = true")
    BigDecimal sumBilledHoursByCaseId(@Param("caseId") Long caseId);

    @Query("SELECT COALESCE(SUM(t.hours * t.hourlyRate), 0) FROM TimeEntry t WHERE t.caseEntity.id = :caseId AND t.billable = true")
    BigDecimal sumBillableAmountByCaseId(@Param("caseId") Long caseId);

    @Query("SELECT COALESCE(SUM(t.hours * t.hourlyRate), 0) FROM TimeEntry t WHERE t.caseEntity.id = :caseId AND t.billed = true")
    BigDecimal sumBilledAmountByCaseId(@Param("caseId") Long caseId);
}
