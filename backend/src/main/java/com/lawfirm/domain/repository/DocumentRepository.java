package com.lawfirm.domain.repository;

import com.lawfirm.domain.model.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {

    @Query("""
        SELECT d FROM Document d
        LEFT JOIN FETCH d.caseEntity
        LEFT JOIN FETCH d.uploadedBy
        WHERE d.caseEntity.id = :caseId
        ORDER BY d.createdAt DESC
        """)
    List<Document> findByCaseId(@Param("caseId") Long caseId);

    @Query("""
        SELECT d FROM Document d
        LEFT JOIN FETCH d.caseEntity
        LEFT JOIN FETCH d.uploadedBy
        WHERE d.id = :id
        """)
    Optional<Document> findByIdWithDetails(@Param("id") Long id);

    @Query("""
        SELECT d FROM Document d
        LEFT JOIN FETCH d.caseEntity
        LEFT JOIN FETCH d.uploadedBy
        WHERE d.caseEntity.id = :caseId
          AND (LOWER(d.title) LIKE LOWER(CONCAT('%', :query, '%'))
            OR LOWER(d.originalFilename) LIKE LOWER(CONCAT('%', :query, '%')))
        ORDER BY d.createdAt DESC
        """)
    List<Document> searchByCaseIdAndQuery(@Param("caseId") Long caseId, @Param("query") String query);
}
