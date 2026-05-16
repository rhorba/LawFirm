package com.lawfirm.application.service;

import com.lawfirm.application.dto.request.InvoiceItemRequest;
import com.lawfirm.application.dto.request.InvoiceRequest;
import com.lawfirm.application.dto.request.InvoiceStatusRequest;
import com.lawfirm.application.dto.response.InvoiceResponse;
import com.lawfirm.application.mapper.InvoiceMapper;
import com.lawfirm.domain.model.Case;
import com.lawfirm.domain.model.FinancialTransaction;
import com.lawfirm.domain.model.Invoice;
import com.lawfirm.domain.model.Invoice.InvoiceStatus;
import com.lawfirm.domain.repository.CaseRepository;
import com.lawfirm.domain.repository.FinancialTransactionRepository;
import com.lawfirm.domain.repository.InvoiceRepository;
import com.lawfirm.presentation.exception.ResourceNotFoundException;
import jakarta.persistence.EntityManager;
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
class InvoiceServiceTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private CaseRepository caseRepository;

    @Mock
    private FinancialTransactionRepository financialTransactionRepository;

    @Mock
    private InvoiceMapper mapper;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private InvoiceService invoiceService;

    private Invoice testInvoice;
    private InvoiceResponse testInvoiceResponse;
    private Case testCase;

    @BeforeEach
    void setUp() {
        testCase = new Case();
        testCase.setId(10L);
        testCase.setFullCaseNumber("PEN-2026/TRB-001/001");

        testInvoice = Invoice.builder()
            .id(1L)
            .caseEntity(testCase)
            .invoiceNumber("FAC-2026-0001")
            .status(InvoiceStatus.DRAFT)
            .subtotal(new BigDecimal("1000.00"))
            .taxAmount(new BigDecimal("200.00"))
            .totalAmount(new BigDecimal("1200.00"))
            .build();

        testInvoiceResponse = mock(InvoiceResponse.class);
        lenient().when(testInvoiceResponse.id()).thenReturn(1L);
        lenient().when(testInvoiceResponse.invoiceNumber()).thenReturn("FAC-2026-0001");
        lenient().when(testInvoiceResponse.status()).thenReturn(InvoiceStatus.DRAFT);
        lenient().when(testInvoiceResponse.totalAmount()).thenReturn(new BigDecimal("1200.00"));
    }

    @Test
    void findById_ShouldReturnInvoice_WhenExists() {
        when(invoiceRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(testInvoice));
        when(mapper.toResponse(testInvoice)).thenReturn(testInvoiceResponse);

        InvoiceResponse result = invoiceService.findById(1L);

        assertThat(result).isNotNull();
        assertThat(result.invoiceNumber()).isEqualTo("FAC-2026-0001");
    }

    @Test
    void findById_ShouldThrow_WhenNotFound() {
        when(invoiceRepository.findByIdAndDeletedAtIsNull(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> invoiceService.findById(99L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Invoice not found");
    }

    @Test
    void updateStatus_ShouldTransitionDraftToSent() {
        InvoiceStatusRequest request = mock(InvoiceStatusRequest.class);
        when(request.status()).thenReturn(InvoiceStatus.SENT);
        when(invoiceRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(testInvoice));
        when(invoiceRepository.save(testInvoice)).thenReturn(testInvoice);
        when(mapper.toResponse(testInvoice)).thenReturn(testInvoiceResponse);

        invoiceService.updateStatus(1L, request);

        assertThat(testInvoice.getStatus()).isEqualTo(InvoiceStatus.SENT);
    }

    @Test
    void updateStatus_ShouldTransitionDraftToCancelled() {
        InvoiceStatusRequest request = mock(InvoiceStatusRequest.class);
        when(request.status()).thenReturn(InvoiceStatus.CANCELLED);
        when(invoiceRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(testInvoice));
        when(invoiceRepository.save(testInvoice)).thenReturn(testInvoice);
        when(mapper.toResponse(testInvoice)).thenReturn(testInvoiceResponse);

        invoiceService.updateStatus(1L, request);

        assertThat(testInvoice.getStatus()).isEqualTo(InvoiceStatus.CANCELLED);
    }

    @Test
    void updateStatus_ShouldReject_WhenDraftToDirectPaid() {
        InvoiceStatusRequest request = mock(InvoiceStatusRequest.class);
        when(request.status()).thenReturn(InvoiceStatus.PAID);
        when(invoiceRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(testInvoice));

        assertThatThrownBy(() -> invoiceService.updateStatus(1L, request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Cannot transition");
    }

    @Test
    void updateStatus_ShouldCreateTransaction_WhenMarkingAsPaid() {
        testInvoice.setStatus(InvoiceStatus.SENT);
        InvoiceStatusRequest request = mock(InvoiceStatusRequest.class);
        when(request.status()).thenReturn(InvoiceStatus.PAID);
        when(request.paymentMode()).thenReturn(FinancialTransaction.PaymentMode.TRANSFER);
        when(request.paymentDate()).thenReturn(LocalDate.now());
        when(invoiceRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(testInvoice));
        when(invoiceRepository.save(testInvoice)).thenReturn(testInvoice);
        when(mapper.toResponse(testInvoice)).thenReturn(testInvoiceResponse);

        invoiceService.updateStatus(1L, request);

        verify(financialTransactionRepository).save(any(FinancialTransaction.class));
        assertThat(testInvoice.getStatus()).isEqualTo(InvoiceStatus.PAID);
    }

    @Test
    void updateStatus_ShouldThrow_WhenPaidMissingPaymentMode() {
        testInvoice.setStatus(InvoiceStatus.SENT);
        InvoiceStatusRequest request = mock(InvoiceStatusRequest.class);
        when(request.status()).thenReturn(InvoiceStatus.PAID);
        when(request.paymentMode()).thenReturn(null);
        when(invoiceRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(testInvoice));

        assertThatThrownBy(() -> invoiceService.updateStatus(1L, request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("paymentMode and paymentDate are required");
    }

    @Test
    void softDelete_ShouldSetDeletedAt() {
        when(invoiceRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(testInvoice));
        when(invoiceRepository.save(testInvoice)).thenReturn(testInvoice);

        invoiceService.softDelete(1L);

        assertThat(testInvoice.getDeletedAt()).isNotNull();
    }

    @Test
    void softDelete_ShouldThrow_WhenNotFound() {
        when(invoiceRepository.findByIdAndDeletedAtIsNull(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> invoiceService.softDelete(99L))
            .isInstanceOf(ResourceNotFoundException.class);
    }
}
