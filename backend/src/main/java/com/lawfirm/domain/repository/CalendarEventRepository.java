package com.lawfirm.domain.repository;

import com.lawfirm.domain.model.CalendarEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CalendarEventRepository extends JpaRepository<CalendarEvent, Long> {

    @Query("SELECT e FROM CalendarEvent e " +
           "LEFT JOIN FETCH e.caseEntity " +
           "LEFT JOIN FETCH e.createdBy " +
           "WHERE e.startDatetime BETWEEN :from AND :to " +
           "ORDER BY e.startDatetime ASC")
    List<CalendarEvent> findByDateRange(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    @Query("SELECT e FROM CalendarEvent e " +
           "LEFT JOIN FETCH e.caseEntity " +
           "LEFT JOIN FETCH e.createdBy " +
           "WHERE e.caseEntity.id = :caseId " +
           "ORDER BY e.startDatetime ASC")
    List<CalendarEvent> findByCaseId(@Param("caseId") Long caseId);

    @Query("SELECT e FROM CalendarEvent e " +
           "LEFT JOIN FETCH e.caseEntity " +
           "LEFT JOIN FETCH e.createdBy " +
           "WHERE e.id = :id")
    Optional<CalendarEvent> findByIdWithDetails(@Param("id") Long id);
}
