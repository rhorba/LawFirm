package com.lawfirm.application.service;

import com.lawfirm.application.dto.request.TimeEntryRequest;
import com.lawfirm.application.dto.response.TimeEntryResponse;
import com.lawfirm.application.dto.response.TimeEntrySummaryResponse;
import com.lawfirm.application.mapper.TimeEntryMapper;
import com.lawfirm.domain.model.Case;
import com.lawfirm.domain.model.Invoice;
import com.lawfirm.domain.model.Lawyer;
import com.lawfirm.domain.model.TimeEntry;
import com.lawfirm.domain.repository.CaseRepository;
import com.lawfirm.domain.repository.InvoiceRepository;
import com.lawfirm.domain.repository.LawyerRepository;
import com.lawfirm.domain.repository.TimeEntryRepository;
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
class TimeEntryServiceTest {

    @Mock private TimeEntryRepository timeEntryRepository;
    @Mock private CaseRepository caseRepository;
    @Mock private LawyerRepository lawyerRepository;
    @Mock private InvoiceRepository invoiceRepository;
    @Mock private TimeEntryMapper timeEntryMapper;

    @InjectMocks
    private TimeEntryService timeEntryService;

    private TimeEntry testEntry;
    private TimeEntryResponse testResponse;
    private Case testCase;
    private Lawyer testLawyer;

    @BeforeEach
    void setUp() {
        testCase = new Case();
        testCase.setId(10L);

        testLawyer = new Lawyer();
        testLawyer.setId(5L);

        testEntry = TimeEntry.builder()
            .id(1L)
            .caseEntity(testCase)
            .lawyer(testLawyer)
            .entryDate(LocalDate.now())
            .hours(new BigDecimal("2.5"))
            .hourlyRate(new BigDecimal("500.00"))
            .billable(true)
            .billed(false)
            .build();

        testResponse = mock(TimeEntryResponse.class);
        lenient().when(testResponse.id()).thenReturn(1L);
        lenient().when(testResponse.hours()).thenReturn(new BigDecimal("2.5"));
        lenient().when(testResponse.billable()).thenReturn(true);
        lenient().when(testResponse.billed()).thenReturn(false);
    }

    @Test
    void findByCaseId_ShouldReturnEntries() {
        when(timeEntryRepository.findByCaseId(10L)).thenReturn(List.of(testEntry));
        when(timeEntryMapper.toResponseList(anyList())).thenReturn(List.of(testResponse));

        List<TimeEntryResponse> result = timeEntryService.findByCaseId(10L);

        assertThat(result).hasSize(1);
    }

    @Test
    void getSummary_ShouldAggregateCorrectly() {
        when(timeEntryRepository.sumHoursByCaseId(10L)).thenReturn(new BigDecimal("10.0"));
        when(timeEntryRepository.sumBillableHoursByCaseId(10L)).thenReturn(new BigDecimal("8.0"));
        when(timeEntryRepository.sumBilledHoursByCaseId(10L)).thenReturn(new BigDecimal("3.0"));
        when(timeEntryRepository.sumBillableAmountByCaseId(10L)).thenReturn(new BigDecimal("4000.00"));
        when(timeEntryRepository.sumBilledAmountByCaseId(10L)).thenReturn(new BigDecimal("1500.00"));

        TimeEntrySummaryResponse summary = timeEntryService.getSummary(10L);

        assertThat(summary.totalHours()).isEqualByComparingTo("10.0");
        assertThat(summary.billableHours()).isEqualByComparingTo("8.0");
        assertThat(summary.billedHours()).isEqualByComparingTo("3.0");
        assertThat(summary.unbilledHours()).isEqualByComparingTo("5.0");
        assertThat(summary.billableAmount()).isEqualByComparingTo("4000.00");
        assertThat(summary.billedAmount()).isEqualByComparingTo("1500.00");
        assertThat(summary.unbilledAmount()).isEqualByComparingTo("2500.00");
    }

    @Test
    void create_ShouldSaveEntry() {
        TimeEntryRequest request = mock(TimeEntryRequest.class);
        when(request.lawyerId()).thenReturn(5L);
        when(request.entryDate()).thenReturn(LocalDate.now());
        when(request.hours()).thenReturn(new BigDecimal("2.5"));
        when(request.hourlyRate()).thenReturn(new BigDecimal("500.00"));
        when(request.billable()).thenReturn(true);

        when(caseRepository.findById(10L)).thenReturn(Optional.of(testCase));
        when(lawyerRepository.findById(5L)).thenReturn(Optional.of(testLawyer));
        when(timeEntryRepository.save(any(TimeEntry.class))).thenReturn(testEntry);
        when(timeEntryMapper.toResponse(testEntry)).thenReturn(testResponse);

        TimeEntryResponse result = timeEntryService.create(10L, request);

        assertThat(result).isNotNull();
        verify(timeEntryRepository).save(any(TimeEntry.class));
    }

    @Test
    void create_ShouldThrow_WhenCaseNotFound() {
        TimeEntryRequest request = mock(TimeEntryRequest.class);
        when(caseRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> timeEntryService.create(99L, request))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Case not found");
    }

    @Test
    void create_ShouldThrow_WhenLawyerNotFound() {
        TimeEntryRequest request = mock(TimeEntryRequest.class);
        when(request.lawyerId()).thenReturn(99L);
        when(caseRepository.findById(10L)).thenReturn(Optional.of(testCase));
        when(lawyerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> timeEntryService.create(10L, request))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Lawyer not found");
    }

    @Test
    void delete_ShouldCallRepository_WhenExists() {
        when(timeEntryRepository.existsById(1L)).thenReturn(true);

        timeEntryService.delete(1L);

        verify(timeEntryRepository).deleteById(1L);
    }

    @Test
    void delete_ShouldThrow_WhenNotFound() {
        when(timeEntryRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> timeEntryService.delete(99L))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void markAsBilled_ShouldSetBilledTrue() {
        when(timeEntryRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(testEntry));
        when(timeEntryRepository.save(testEntry)).thenReturn(testEntry);
        when(timeEntryMapper.toResponse(testEntry)).thenReturn(testResponse);

        timeEntryService.markAsBilled(1L, null);

        assertThat(testEntry.isBilled()).isTrue();
    }

    @Test
    void markAsBilled_ShouldLinkInvoice_WhenInvoiceIdProvided() {
        Invoice invoice = Invoice.builder().id(20L).build();
        when(timeEntryRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(testEntry));
        when(invoiceRepository.findById(20L)).thenReturn(Optional.of(invoice));
        when(timeEntryRepository.save(testEntry)).thenReturn(testEntry);
        when(timeEntryMapper.toResponse(testEntry)).thenReturn(testResponse);

        timeEntryService.markAsBilled(1L, 20L);

        assertThat(testEntry.getInvoice()).isEqualTo(invoice);
    }
}
