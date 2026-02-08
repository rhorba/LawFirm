package com.boilerplate.domain.repository;

import com.boilerplate.domain.model.FinancialTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface FinancialTransactionRepository extends JpaRepository<FinancialTransaction, Long> {

    List<FinancialTransaction> findByCaseEntityId(Long caseId);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM FinancialTransaction t " +
           "WHERE t.caseEntity.id = :caseId AND t.transactionType = 'PAYMENT'")
    BigDecimal sumPaymentsByCaseId(@Param("caseId") Long caseId);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM FinancialTransaction t " +
           "WHERE t.caseEntity.id = :caseId AND t.transactionType = 'EXPENSE'")
    BigDecimal sumExpensesByCaseId(@Param("caseId") Long caseId);
}
