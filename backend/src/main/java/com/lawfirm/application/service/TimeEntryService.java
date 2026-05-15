package com.lawfirm.application.service;

import com.lawfirm.application.dto.request.TimeEntryRequest;
import com.lawfirm.application.dto.response.TimeEntryResponse;
import com.lawfirm.application.dto.response.TimeEntrySummaryResponse;
import com.lawfirm.application.mapper.TimeEntryMapper;
import com.lawfirm.domain.model.TimeEntry;
import com.lawfirm.domain.repository.CaseRepository;
import com.lawfirm.domain.repository.InvoiceRepository;
import com.lawfirm.domain.repository.LawyerRepository;
import com.lawfirm.domain.repository.TimeEntryRepository;
import com.lawfirm.presentation.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TimeEntryService {

    private final TimeEntryRepository timeEntryRepository;
    private final CaseRepository caseRepository;
    private final LawyerRepository lawyerRepository;
    private final InvoiceRepository invoiceRepository;
    private final TimeEntryMapper timeEntryMapper;

    // ── Queries ───────────────────────────────────────────────────────────────

    public List<TimeEntryResponse> findByCaseId(Long caseId) {
        return timeEntryMapper.toResponseList(timeEntryRepository.findByCaseId(caseId));
    }

    public TimeEntrySummaryResponse getSummary(Long caseId) {
        BigDecimal totalHours    = timeEntryRepository.sumHoursByCaseId(caseId);
        BigDecimal billableHours = timeEntryRepository.sumBillableHoursByCaseId(caseId);
        BigDecimal billedHours   = timeEntryRepository.sumBilledHoursByCaseId(caseId);
        BigDecimal unbilledHours = billableHours.subtract(billedHours);
        BigDecimal billableAmt   = timeEntryRepository.sumBillableAmountByCaseId(caseId);
        BigDecimal billedAmt     = timeEntryRepository.sumBilledAmountByCaseId(caseId);
        BigDecimal unbilledAmt   = billableAmt.subtract(billedAmt);
        return new TimeEntrySummaryResponse(
            totalHours, billableHours, billedHours, unbilledHours,
            billableAmt, billedAmt, unbilledAmt);
    }

    // ── CRUD ──────────────────────────────────────────────────────────────────

    @Transactional
    public TimeEntryResponse create(Long caseId, TimeEntryRequest request) {
        var caseEntity = caseRepository.findById(caseId)
            .orElseThrow(() -> new ResourceNotFoundException("Case not found: " + caseId));
        var lawyer = lawyerRepository.findById(request.lawyerId())
            .orElseThrow(() -> new ResourceNotFoundException("Lawyer not found: " + request.lawyerId()));

        var entry = TimeEntry.builder()
            .caseEntity(caseEntity)
            .lawyer(lawyer)
            .entryDate(request.entryDate())
            .hours(request.hours())
            .description(request.description())
            .hourlyRate(request.hourlyRate())
            .billable(request.billable())
            .billed(false)
            .build();

        return timeEntryMapper.toResponse(timeEntryRepository.save(entry));
    }

    @Transactional
    public TimeEntryResponse update(Long id, TimeEntryRequest request) {
        var entry = getOrThrow(id);
        var lawyer = lawyerRepository.findById(request.lawyerId())
            .orElseThrow(() -> new ResourceNotFoundException("Lawyer not found: " + request.lawyerId()));

        entry.setLawyer(lawyer);
        entry.setEntryDate(request.entryDate());
        entry.setHours(request.hours());
        entry.setDescription(request.description());
        entry.setHourlyRate(request.hourlyRate());
        entry.setBillable(request.billable());

        return timeEntryMapper.toResponse(timeEntryRepository.save(entry));
    }

    @Transactional
    public void delete(Long id) {
        if (!timeEntryRepository.existsById(id)) {
            throw new ResourceNotFoundException("TimeEntry not found: " + id);
        }
        timeEntryRepository.deleteById(id);
    }

    @Transactional
    public TimeEntryResponse markAsBilled(Long id, Long invoiceId) {
        var entry = getOrThrow(id);
        entry.setBilled(true);
        if (invoiceId != null) {
            entry.setInvoice(invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found: " + invoiceId)));
        }
        return timeEntryMapper.toResponse(timeEntryRepository.save(entry));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private TimeEntry getOrThrow(Long id) {
        return timeEntryRepository.findByIdWithDetails(id)
            .orElseThrow(() -> new ResourceNotFoundException("TimeEntry not found: " + id));
    }
}
