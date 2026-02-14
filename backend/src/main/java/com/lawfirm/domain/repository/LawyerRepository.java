package com.lawfirm.domain.repository;

import com.lawfirm.domain.model.Lawyer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LawyerRepository extends JpaRepository<Lawyer, Long> {

    Optional<Lawyer> findByIdAndActiveTrue(Long id);

    Optional<Lawyer> findByTaxId(String taxId);

    List<Lawyer> findAllByActiveTrue();

    @Query("SELECT l FROM Lawyer l WHERE "
        + "(:search IS NULL OR LOWER(l.firstName) LIKE LOWER(CONCAT('%', :search, '%')) "
        + "OR LOWER(l.lastName) LIKE LOWER(CONCAT('%', :search, '%')) "
        + "OR LOWER(l.taxId) LIKE LOWER(CONCAT('%', :search, '%')) "
        + "OR LOWER(l.email) LIKE LOWER(CONCAT('%', :search, '%')) "
        + "OR LOWER(l.phone) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Lawyer> search(@Param("search") String search, Pageable pageable);

    @Query("SELECT COUNT(c) FROM Case c WHERE c.lawyer.id = :lawyerId AND c.deletedAt IS NULL")
    Long countActiveCases(@Param("lawyerId") Long lawyerId);
}
