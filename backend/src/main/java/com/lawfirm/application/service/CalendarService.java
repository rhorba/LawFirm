package com.lawfirm.application.service;

import com.lawfirm.application.dto.request.CalendarEventRequest;
import com.lawfirm.application.dto.response.CalendarDayEvent;
import com.lawfirm.application.dto.response.CalendarEventResponse;
import com.lawfirm.application.mapper.CalendarEventMapper;
import com.lawfirm.domain.model.CalendarEvent;
import com.lawfirm.domain.model.Task.TaskStatus;
import com.lawfirm.domain.repository.CalendarEventRepository;
import com.lawfirm.domain.repository.CaseRepository;
import com.lawfirm.domain.repository.TaskRepository;
import com.lawfirm.domain.repository.UserRepository;
import com.lawfirm.presentation.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CalendarService {

    private final CalendarEventRepository calendarEventRepository;
    private final TaskRepository taskRepository;
    private final CaseRepository caseRepository;
    private final UserRepository userRepository;
    private final CalendarEventMapper calendarEventMapper;

    // ── Month view (merged calendar events + task deadlines) ──────────────────

    public List<CalendarDayEvent> getMonthEvents(int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        LocalDateTime from = ym.atDay(1).atStartOfDay();
        LocalDateTime to = ym.atEndOfMonth().atTime(23, 59, 59);

        List<CalendarDayEvent> result = new ArrayList<>();

        // Calendar events
        calendarEventRepository.findByDateRange(from, to).forEach(e -> result.add(
            new CalendarDayEvent(
                e.getId(),
                e.getTitle(),
                e.getEventType().name(),
                "CALENDAR_EVENT",
                e.getStartDatetime().toLocalDate(),
                e.getCaseEntity() != null ? e.getCaseEntity().getId() : null,
                e.getCaseEntity() != null ? e.getCaseEntity().getFullCaseNumber() : null
            )
        ));

        // Task due dates (exclude DONE / CANCELLED)
        LocalDate dateFrom = ym.atDay(1);
        LocalDate dateTo = ym.atEndOfMonth();
        taskRepository.findUpcoming(dateFrom, dateTo).stream()
            .filter(t -> t.getStatus() != TaskStatus.DONE && t.getStatus() != TaskStatus.CANCELLED)
            .forEach(t -> result.add(
                new CalendarDayEvent(
                    t.getId(),
                    t.getTitle(),
                    "TASK",
                    "TASK",
                    t.getDueDate(),
                    t.getCaseEntity() != null ? t.getCaseEntity().getId() : null,
                    t.getCaseEntity() != null ? t.getCaseEntity().getFullCaseNumber() : null
                )
            ));

        result.sort(Comparator.comparing(CalendarDayEvent::date));
        return result;
    }

    // ── CRUD ──────────────────────────────────────────────────────────────────

    public List<CalendarEventResponse> findByCaseId(Long caseId) {
        return calendarEventMapper.toResponseList(calendarEventRepository.findByCaseId(caseId));
    }

    public CalendarEventResponse findById(Long id) {
        return calendarEventMapper.toResponse(getEventOrThrow(id));
    }

    @Transactional
    public CalendarEventResponse create(CalendarEventRequest request) {
        var event = CalendarEvent.builder()
            .title(request.title())
            .description(request.description())
            .eventType(request.eventType())
            .startDatetime(request.startDatetime())
            .endDatetime(request.endDatetime())
            .allDay(request.allDay())
            .createdBy(getCurrentUser())
            .build();

        if (request.caseId() != null) {
            event.setCaseEntity(caseRepository.findById(request.caseId())
                .orElseThrow(() -> new ResourceNotFoundException("Case not found: " + request.caseId())));
        }

        return calendarEventMapper.toResponse(calendarEventRepository.save(event));
    }

    @Transactional
    public CalendarEventResponse update(Long id, CalendarEventRequest request) {
        var event = getEventOrThrow(id);

        event.setTitle(request.title());
        event.setDescription(request.description());
        event.setEventType(request.eventType());
        event.setStartDatetime(request.startDatetime());
        event.setEndDatetime(request.endDatetime());
        event.setAllDay(request.allDay());

        if (request.caseId() != null) {
            event.setCaseEntity(caseRepository.findById(request.caseId())
                .orElseThrow(() -> new ResourceNotFoundException("Case not found: " + request.caseId())));
        } else {
            event.setCaseEntity(null);
        }

        return calendarEventMapper.toResponse(calendarEventRepository.save(event));
    }

    @Transactional
    public void delete(Long id) {
        if (!calendarEventRepository.existsById(id)) {
            throw new ResourceNotFoundException("CalendarEvent not found: " + id);
        }
        calendarEventRepository.deleteById(id);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private CalendarEvent getEventOrThrow(Long id) {
        return calendarEventRepository.findByIdWithDetails(id)
            .orElseThrow(() -> new ResourceNotFoundException("CalendarEvent not found: " + id));
    }

    private com.lawfirm.domain.model.User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsernameAndDeletedAtIsNull(username)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
    }
}
