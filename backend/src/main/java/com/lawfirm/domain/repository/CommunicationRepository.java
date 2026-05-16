package com.lawfirm.domain.repository;

import com.lawfirm.domain.model.Communication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CommunicationRepository extends JpaRepository<Communication, Long> {

    @Query("""
        SELECT c FROM Communication c
        LEFT JOIN FETCH c.caseEntity
        LEFT JOIN FETCH c.sentBy
        WHERE c.caseEntity.id = :caseId
        ORDER BY c.createdAt DESC
        """)
    List<Communication> findByCaseId(@Param("caseId") Long caseId);

    @Query("""
        SELECT c FROM Communication c
        LEFT JOIN FETCH c.caseEntity
        LEFT JOIN FETCH c.sentBy
        WHERE c.id = :id
        """)
    Optional<Communication> findByIdWithDetails(@Param("id") Long id);
}
