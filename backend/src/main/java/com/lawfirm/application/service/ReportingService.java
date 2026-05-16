package com.lawfirm.application.service;

import com.lawfirm.application.dto.response.ChartDataResponse;
import com.lawfirm.application.dto.response.LawyerWorkloadEntry;
import com.lawfirm.application.dto.response.ReportSummaryResponse;
import com.lawfirm.application.dto.response.UnpaidInvoiceEntry;
import com.lawfirm.domain.model.FinancialTransaction;
import com.lawfirm.domain.model.Invoice;
import com.lawfirm.domain.model.Task;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ReportingService {

    @PersistenceContext
    private EntityManager em;

    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM");

    // ── Summary KPIs ────────────────────────────────────────────────────────────

    public ReportSummaryResponse getSummary(LocalDate from, LocalDate to) {
        long totalCases = count(
            "SELECT COUNT(c) FROM Case c WHERE c.deletedAt IS NULL AND c.registrationDate BETWEEN :from AND :to",
            from, to
        );
        long openCases = count(
            "SELECT COUNT(c) FROM Case c WHERE c.deletedAt IS NULL AND c.status.isTerminal = false AND c.registrationDate BETWEEN :from AND :to",
            from, to
        );
        long overdueTasks = (long) em.createQuery(
                "SELECT COUNT(t) FROM Task t WHERE t.status NOT IN :done AND t.dueDate < :today")
            .setParameter("done", List.of(Task.TaskStatus.DONE, Task.TaskStatus.CANCELLED))
            .setParameter("today", LocalDate.now())
            .getSingleResult();

        BigDecimal totalRevenue = sumOrZero(
            "SELECT COALESCE(SUM(t.amount), 0) FROM FinancialTransaction t WHERE t.deletedAt IS NULL AND t.direction = :dir AND t.paymentDate BETWEEN :from AND :to",
            from, to, "dir", FinancialTransaction.Direction.REVENUE
        );

        BigDecimal unbilledHours = (BigDecimal) em.createQuery(
                "SELECT COALESCE(SUM(te.hours), 0) FROM TimeEntry te WHERE te.billable = true AND te.billed = false")
            .getSingleResult();

        BigDecimal unbilledAmount = (BigDecimal) em.createQuery(
                "SELECT COALESCE(SUM(te.hours * te.hourlyRate), 0) FROM TimeEntry te WHERE te.billable = true AND te.billed = false")
            .getSingleResult();

        long totalDocuments = (long) em.createQuery("SELECT COUNT(d) FROM Document d")
            .getSingleResult();

        long pendingInvoices = (long) em.createQuery(
                "SELECT COUNT(i) FROM Invoice i WHERE i.deletedAt IS NULL AND i.status IN :statuses")
            .setParameter("statuses", List.of(Invoice.InvoiceStatus.SENT))
            .getSingleResult();

        return new ReportSummaryResponse(
            totalCases, openCases, overdueTasks,
            totalRevenue, unbilledHours, unbilledAmount,
            totalDocuments, pendingInvoices
        );
    }

    // ── Cases by status ─────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    public ChartDataResponse getCasesByStatus(LocalDate from, LocalDate to) {
        List<Object[]> rows = em.createQuery(
                "SELECT c.status.nameFr, COUNT(c) FROM Case c WHERE c.deletedAt IS NULL AND c.registrationDate BETWEEN :from AND :to GROUP BY c.status.nameFr ORDER BY COUNT(c) DESC")
            .setParameter("from", from)
            .setParameter("to", to)
            .getResultList();

        List<String> labels = rows.stream().map(r -> (String) r[0]).toList();
        List<Number> values = rows.stream().map(r -> (Number) r[1]).toList();
        return new ChartDataResponse(labels, values);
    }

    // ── Cases by month ───────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    public ChartDataResponse getCasesByMonth(LocalDate from, LocalDate to) {
        List<Object[]> rows = em.createQuery(
                "SELECT c.registrationDate, COUNT(c) FROM Case c WHERE c.deletedAt IS NULL AND c.registrationDate BETWEEN :from AND :to GROUP BY c.registrationDate ORDER BY c.registrationDate")
            .setParameter("from", from)
            .setParameter("to", to)
            .getResultList();

        // Group by year-month in Java
        Map<String, Long> byMonth = new LinkedHashMap<>();
        for (Object[] row : rows) {
            LocalDate date = (LocalDate) row[0];
            String month = date.format(MONTH_FMT);
            byMonth.merge(month, (Long) row[1], Long::sum);
        }

        // Fill gaps for all months in range
        Map<String, Long> filled = buildMonthRange(from, to, byMonth);
        return new ChartDataResponse(new ArrayList<>(filled.keySet()), new ArrayList<>(filled.values()));
    }

    // ── Financial by month ───────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    public ChartDataResponse getFinancialByMonth(LocalDate from, LocalDate to) {
        List<Object[]> rows = em.createQuery(
                "SELECT t.paymentDate, t.direction, SUM(t.amount) FROM FinancialTransaction t WHERE t.deletedAt IS NULL AND t.paymentDate BETWEEN :from AND :to GROUP BY t.paymentDate, t.direction ORDER BY t.paymentDate")
            .setParameter("from", from)
            .setParameter("to", to)
            .getResultList();

        Map<String, BigDecimal> revenue = new LinkedHashMap<>();
        Map<String, BigDecimal> expense = new LinkedHashMap<>();
        for (Object[] row : rows) {
            LocalDate date = (LocalDate) row[0];
            String month = date.format(MONTH_FMT);
            BigDecimal amount = (BigDecimal) row[2];
            if (FinancialTransaction.Direction.REVENUE.name().equals(row[1].toString())) {
                revenue.merge(month, amount, BigDecimal::add);
            } else {
                expense.merge(month, amount, BigDecimal::add);
            }
        }

        Map<String, Long> empty = buildMonthRange(from, to, Map.of());
        List<String> labels = new ArrayList<>(empty.keySet());
        List<Number> revValues = labels.stream().map(m -> (Number) revenue.getOrDefault(m, BigDecimal.ZERO)).toList();
        List<Number> expValues = labels.stream().map(m -> (Number) expense.getOrDefault(m, BigDecimal.ZERO)).toList();
        return new ChartDataResponse(labels, revValues, expValues);
    }

    // ── Lawyer workload ──────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    public List<LawyerWorkloadEntry> getLawyerWorkload(LocalDate from, LocalDate to) {
        List<Object[]> rows = em.createQuery(
                "SELECT l.id, l.firstName, l.lastName, " +
                "COALESCE(SUM(te.hours), 0), " +
                "COALESCE(SUM(CASE WHEN te.billable = true THEN te.hours ELSE 0 END), 0), " +
                "COALESCE(SUM(CASE WHEN te.billable = true THEN te.hours * te.hourlyRate ELSE 0 END), 0) " +
                "FROM Lawyer l " +
                "LEFT JOIN TimeEntry te ON te.lawyer.id = l.id AND te.entryDate BETWEEN :from AND :to " +
                "WHERE l.active = true " +
                "GROUP BY l.id, l.firstName, l.lastName " +
                "ORDER BY SUM(te.hours) DESC NULLS LAST")
            .setParameter("from", from)
            .setParameter("to", to)
            .getResultList();

        return rows.stream().map(r -> new LawyerWorkloadEntry(
            (Long) r[0],
            r[1] + " " + r[2],
            0L,
            (BigDecimal) r[3],
            (BigDecimal) r[4],
            (BigDecimal) r[5]
        )).toList();
    }

    // ── Top unpaid invoices ──────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    public List<UnpaidInvoiceEntry> getTopUnpaidInvoices() {
        List<Object[]> rows = em.createQuery(
                "SELECT i.id, i.invoiceNumber, i.caseEntity.id, i.caseEntity.fullCaseNumber, i.totalAmount, i.dueDate, i.status " +
                "FROM Invoice i " +
                "WHERE i.deletedAt IS NULL AND i.status = :status " +
                "ORDER BY i.totalAmount DESC")
            .setParameter("status", Invoice.InvoiceStatus.SENT)
            .setMaxResults(10)
            .getResultList();

        return rows.stream().map(r -> new UnpaidInvoiceEntry(
            (Long) r[0],
            (String) r[1],
            (Long) r[2],
            (String) r[3],
            (BigDecimal) r[4],
            (LocalDate) r[5],
            r[6].toString()
        )).toList();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────────

    private long count(String jpql, LocalDate from, LocalDate to) {
        return (long) em.createQuery(jpql)
            .setParameter("from", from)
            .setParameter("to", to)
            .getSingleResult();
    }

    private BigDecimal sumOrZero(String jpql, LocalDate from, LocalDate to, String extraParam, Object extraValue) {
        Object result = em.createQuery(jpql)
            .setParameter("from", from)
            .setParameter("to", to)
            .setParameter(extraParam, extraValue)
            .getSingleResult();
        return result instanceof BigDecimal bd ? bd : BigDecimal.ZERO;
    }

    private Map<String, Long> buildMonthRange(LocalDate from, LocalDate to, Map<String, ?> existing) {
        Map<String, Long> result = new LinkedHashMap<>();
        LocalDate cursor = from.withDayOfMonth(1);
        LocalDate end = to.withDayOfMonth(1);
        while (!cursor.isAfter(end)) {
            String key = cursor.format(MONTH_FMT);
            Object v = existing.get(key);
            result.put(key, v instanceof Long l ? l : 0L);
            cursor = cursor.plusMonths(1);
        }
        return result;
    }
}
