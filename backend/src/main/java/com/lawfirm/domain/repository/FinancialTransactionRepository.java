package com.lawfirm.domain.repository;

import com.lawfirm.domain.model.FinancialTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface FinancialTransactionRepository
        extends JpaRepository<FinancialTransaction, Long>,
                JpaSpecificationExecutor<FinancialTransaction> {

    List<FinancialTransaction> findByCaseEntityIdAndDeletedAtIsNull(Long caseId);

    Page<FinancialTransaction> findAllByDeletedAtIsNull(Pageable pageable);

    Optional<FinancialTransaction> findByIdAndDeletedAtIsNull(Long id);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM FinancialTransaction t " +
           "WHERE t.caseEntity.id = :caseId " +
           "AND t.direction = com.lawfirm.domain.model.FinancialTransaction.Direction.REVENUE " +
           "AND t.deletedAt IS NULL")
    BigDecimal sumRevenueByCaseId(@Param("caseId") Long caseId);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM FinancialTransaction t " +
           "WHERE t.caseEntity.id = :caseId " +
           "AND t.direction = com.lawfirm.domain.model.FinancialTransaction.Direction.EXPENSE " +
           "AND t.deletedAt IS NULL")
    BigDecimal sumExpensesByCaseId(@Param("caseId") Long caseId);

    @Query("SELECT COUNT(t) FROM FinancialTransaction t " +
           "WHERE t.caseEntity.id = :caseId AND t.deletedAt IS NULL")
    int countByCaseId(@Param("caseId") Long caseId);
}
