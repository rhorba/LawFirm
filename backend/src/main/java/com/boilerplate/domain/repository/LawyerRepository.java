package com.boilerplate.domain.repository;

import com.boilerplate.domain.model.Lawyer;
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

    @Query("SELECT COUNT(c) FROM Case c WHERE c.lawyer.id = :lawyerId AND c.deletedAt IS NULL")
    Long countActiveCases(@Param("lawyerId") Long lawyerId);
}
