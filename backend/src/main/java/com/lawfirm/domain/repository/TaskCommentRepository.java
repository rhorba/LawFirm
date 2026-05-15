package com.lawfirm.domain.repository;

import com.lawfirm.domain.model.TaskComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskCommentRepository extends JpaRepository<TaskComment, Long> {

    @Query("SELECT c FROM TaskComment c " +
           "LEFT JOIN FETCH c.author " +
           "WHERE c.task.id = :taskId " +
           "ORDER BY c.createdAt ASC")
    List<TaskComment> findByTaskIdWithAuthor(@Param("taskId") Long taskId);
}
