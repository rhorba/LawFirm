package com.lawfirm.domain.repository;

import com.lawfirm.domain.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    @Query("SELECT t FROM Task t " +
           "LEFT JOIN FETCH t.assignedLawyer " +
           "LEFT JOIN FETCH t.createdBy " +
           "WHERE t.caseEntity.id = :caseId " +
           "ORDER BY t.dueDate ASC")
    List<Task> findByCaseIdWithDetails(@Param("caseId") Long caseId);

    @Query("SELECT t FROM Task t " +
           "LEFT JOIN FETCH t.caseEntity c " +
           "LEFT JOIN FETCH t.assignedLawyer " +
           "WHERE t.dueDate BETWEEN :from AND :to " +
           "AND t.status NOT IN ('DONE', 'CANCELLED') " +
           "ORDER BY t.dueDate ASC")
    List<Task> findUpcoming(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("SELECT t FROM Task t " +
           "LEFT JOIN FETCH t.assignedLawyer " +
           "LEFT JOIN FETCH t.createdBy " +
           "LEFT JOIN FETCH t.caseEntity " +
           "WHERE t.id = :id")
    Optional<Task> findByIdWithDetails(@Param("id") Long id);
}
