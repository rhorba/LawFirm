package com.lawfirm.domain.repository;

import com.lawfirm.domain.model.ConflictCheck;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ConflictCheckRepository extends JpaRepository<ConflictCheck, Long> {

    @Query("SELECT c FROM ConflictCheck c " +
           "LEFT JOIN FETCH c.performedBy " +
           "LEFT JOIN FETCH c.clearedBy " +
           "ORDER BY c.createdAt DESC")
    Page<ConflictCheck> findAllWithUsers(Pageable pageable);

    @Query("SELECT c FROM ConflictCheck c " +
           "LEFT JOIN FETCH c.performedBy " +
           "LEFT JOIN FETCH c.clearedBy " +
           "WHERE c.id = :id")
    java.util.Optional<ConflictCheck> findByIdWithUsers(@Param("id") Long id);
}
