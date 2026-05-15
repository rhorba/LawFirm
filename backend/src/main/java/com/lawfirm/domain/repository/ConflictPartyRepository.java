package com.lawfirm.domain.repository;

import com.lawfirm.domain.model.ConflictParty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConflictPartyRepository extends JpaRepository<ConflictParty, Long> {

    @Query("SELECT p FROM ConflictParty p " +
           "LEFT JOIN FETCH p.caseEntity " +
           "WHERE p.caseEntity.id = :caseId " +
           "ORDER BY p.partyType, p.name")
    List<ConflictParty> findByCaseId(@Param("caseId") Long caseId);

    @Query("SELECT p FROM ConflictParty p " +
           "LEFT JOIN FETCH p.caseEntity " +
           "WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<ConflictParty> searchByName(@Param("name") String name);
}
