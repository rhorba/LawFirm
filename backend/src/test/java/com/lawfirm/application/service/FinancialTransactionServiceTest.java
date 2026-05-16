package com.lawfirm.application.service;

import com.lawfirm.application.dto.request.FinancialFilterRequest;
import com.lawfirm.application.dto.request.FinancialTransactionRequest;
import com.lawfirm.application.dto.response.FinancialSummary;
import com.lawfirm.application.dto.response.FinancialTransactionResponse;
import com.lawfirm.application.mapper.FinancialTransactionMapper;
import com.lawfirm.domain.model.Case;
import com.lawfirm.domain.model.FinancialTransaction;
import com.lawfirm.domain.model.FinancialTransaction.Direction;
import com.lawfirm.domain.model.FinancialTransaction.OperationType;
import com.lawfirm.domain.repository.CaseRepository;
import com.lawfirm.domain.repository.FinancialTransactionRepository;
import com.lawfirm.presentation.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FinancialTransactionServiceTest {

    @Mock private FinancialTransactionRepository transactionRepository;
    @Mock private CaseRepository caseRepository;
    @Mock private FinancialTransactionMapper mapper;

    @InjectMocks
    private FinancialTransactionService financialTransactionService;

    private FinancialTransaction testTx;
    private FinancialTransactionResponse testResponse;
    private Case testCase;

    @BeforeEach
    void setUp() {
        testCase = new Case();
        testCase.setId(10L);
        testCase.setFullCaseNumber("PEN-2026/TRB-001/001");

        testTx = FinancialTransaction.builder()
            .id(1L)
            .caseEntity(testCase)
            .direction(Direction.REVENUE)
            .operationType(OperationType.OPENING_FEE)
            .amount(new BigDecimal("5000.00"))
            .paymentDate(LocalDate.now())
            .build();

        testResponse = mock(FinancialTransactionResponse.class);
        lenient().when(testResponse.id()).thenReturn(1L);
        lenient().when(testResponse.amount()).thenReturn(new BigDecimal("5000.00"));
        lenient().when(testResponse.direction()).thenReturn(Direction.REVENUE);
    }

    @Test
    void findByCaseId_ShouldReturnTransactions() {
        when(transactionRepository.findByCaseEntityIdAndDeletedAtIsNull(10L))
            .thenReturn(List.of(testTx));
        when(mapper.toResponse(testTx)).thenReturn(testResponse);

        List<FinancialTransactionResponse> result = financialTransactionService.findByCaseId(10L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).amount()).isEqualByComparingTo("5000.00");
    }

    @Test
    void getSummaryByCaseId_ShouldComputeNetCorrectly() {
        when(transactionRepository.sumRevenueByCaseId(10L)).thenReturn(new BigDecimal("10000.00"));
        when(transactionRepository.sumExpensesByCaseId(10L)).thenReturn(new BigDecimal("3000.00"));
        when(transactionRepository.countByCaseId(10L)).thenReturn(5);

        FinancialSummary summary = financialTransactionService.getSummaryByCaseId(10L);

        assertThat(summary.totalRevenue()).isEqualByComparingTo("10000.00");
        assertThat(summary.totalExpenses()).isEqualByComparingTo("3000.00");
        assertThat(summary.balance()).isEqualByComparingTo("7000.00");
        assertThat(summary.transactionCount()).isEqualTo(5);
    }

    @Test
    void create_ShouldSaveTransaction() {
        FinancialTransactionRequest request = mock(FinancialTransactionRequest.class);
        when(request.caseId()).thenReturn(10L);
        when(request.direction()).thenReturn(Direction.REVENUE);
        when(request.operationType()).thenReturn(OperationType.OPENING_FEE);
        when(request.amount()).thenReturn(new BigDecimal("5000.00"));
        when(request.paymentDate()).thenReturn(LocalDate.now());

        when(caseRepository.findById(10L)).thenReturn(Optional.of(testCase));
        when(transactionRepository.save(any(FinancialTransaction.class))).thenReturn(testTx);
        when(mapper.toResponse(testTx)).thenReturn(testResponse);

        FinancialTransactionResponse result = financialTransactionService.create(request);

        assertThat(result).isNotNull();
        assertThat(result.amount()).isEqualByComparingTo("5000.00");
        verify(transactionRepository).save(any(FinancialTransaction.class));
    }

    @Test
    void create_ShouldThrow_WhenCaseNotFound() {
        FinancialTransactionRequest request = mock(FinancialTransactionRequest.class);
        when(request.caseId()).thenReturn(99L);
        when(caseRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> financialTransactionService.create(request))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Case not found");
    }

    @Test
    void softDelete_ShouldSetDeletedAt() {
        when(transactionRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(testTx));
        when(transactionRepository.save(testTx)).thenReturn(testTx);

        financialTransactionService.softDelete(1L);

        assertThat(testTx.getDeletedAt()).isNotNull();
    }

    @Test
    void softDelete_ShouldThrow_WhenNotFound() {
        when(transactionRepository.findByIdAndDeletedAtIsNull(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> financialTransactionService.softDelete(99L))
            .isInstanceOf(ResourceNotFoundException.class);
    }
}
