package com.lawfirm.application.service;

import com.lawfirm.application.dto.request.CalendarEventRequest;
import com.lawfirm.application.dto.response.CalendarDayEvent;
import com.lawfirm.application.dto.response.CalendarEventResponse;
import com.lawfirm.application.mapper.CalendarEventMapper;
import com.lawfirm.domain.model.CalendarEvent;
import com.lawfirm.domain.model.CalendarEvent.EventType;
import com.lawfirm.domain.model.Case;
import com.lawfirm.domain.model.Task;
import com.lawfirm.domain.model.Task.TaskStatus;
import com.lawfirm.domain.model.User;
import com.lawfirm.domain.repository.CalendarEventRepository;
import com.lawfirm.domain.repository.CaseRepository;
import com.lawfirm.domain.repository.TaskRepository;
import com.lawfirm.domain.repository.UserRepository;
import com.lawfirm.presentation.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CalendarServiceTest {

    @Mock private CalendarEventRepository calendarEventRepository;
    @Mock private TaskRepository taskRepository;
    @Mock private CaseRepository caseRepository;
    @Mock private UserRepository userRepository;
    @Mock private CalendarEventMapper calendarEventMapper;

    @InjectMocks
    private CalendarService calendarService;

    private CalendarEvent testEvent;
    private CalendarEventResponse testResponse;
    private User testUser;
    private Case testCase;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("admin");

        testCase = new Case();
        testCase.setId(10L);
        testCase.setFullCaseNumber("PEN-2026/TRB-001/001");

        testEvent = CalendarEvent.builder()
            .id(1L)
            .title("Hearing at Tribunal")
            .eventType(EventType.HEARING)
            .startDatetime(LocalDateTime.now().plusDays(3))
            .caseEntity(testCase)
            .createdBy(testUser)
            .build();

        testResponse = mock(CalendarEventResponse.class);
        lenient().when(testResponse.id()).thenReturn(1L);
        lenient().when(testResponse.title()).thenReturn("Hearing at Tribunal");
        lenient().when(testResponse.eventType()).thenReturn(EventType.HEARING);

        SecurityContext ctx = mock(SecurityContext.class);
        Authentication auth = mock(Authentication.class);
        lenient().when(ctx.getAuthentication()).thenReturn(auth);
        lenient().when(auth.getName()).thenReturn("admin");
        SecurityContextHolder.setContext(ctx);
    }

    @Test
    void findById_ShouldReturnEvent_WhenExists() {
        when(calendarEventRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(testEvent));
        when(calendarEventMapper.toResponse(testEvent)).thenReturn(testResponse);

        CalendarEventResponse result = calendarService.findById(1L);

        assertThat(result).isNotNull();
        assertThat(result.title()).isEqualTo("Hearing at Tribunal");
    }

    @Test
    void findById_ShouldThrow_WhenNotFound() {
        when(calendarEventRepository.findByIdWithDetails(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> calendarService.findById(99L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("CalendarEvent not found");
    }

    @Test
    void create_ShouldSaveEvent() {
        when(userRepository.findByUsernameAndDeletedAtIsNull("admin"))
            .thenReturn(Optional.of(testUser));
        CalendarEventRequest request = mock(CalendarEventRequest.class);
        when(request.title()).thenReturn("Hearing at Tribunal");
        when(request.eventType()).thenReturn(EventType.HEARING);
        when(request.startDatetime()).thenReturn(LocalDateTime.now().plusDays(3));
        when(request.caseId()).thenReturn(null);
        when(calendarEventRepository.save(any(CalendarEvent.class))).thenReturn(testEvent);
        when(calendarEventMapper.toResponse(testEvent)).thenReturn(testResponse);

        CalendarEventResponse result = calendarService.create(request);

        assertThat(result).isNotNull();
        verify(calendarEventRepository).save(any(CalendarEvent.class));
    }

    @Test
    void create_ShouldLinkCase_WhenCaseIdProvided() {
        when(userRepository.findByUsernameAndDeletedAtIsNull("admin"))
            .thenReturn(Optional.of(testUser));
        CalendarEventRequest request = mock(CalendarEventRequest.class);
        when(request.title()).thenReturn("Hearing");
        when(request.eventType()).thenReturn(EventType.HEARING);
        when(request.caseId()).thenReturn(10L);
        when(caseRepository.findById(10L)).thenReturn(Optional.of(testCase));
        when(calendarEventRepository.save(any(CalendarEvent.class))).thenReturn(testEvent);
        when(calendarEventMapper.toResponse(testEvent)).thenReturn(testResponse);

        calendarService.create(request);

        verify(caseRepository).findById(10L);
    }

    @Test
    void delete_ShouldCallRepository_WhenExists() {
        when(calendarEventRepository.existsById(1L)).thenReturn(true);

        calendarService.delete(1L);

        verify(calendarEventRepository).deleteById(1L);
    }

    @Test
    void delete_ShouldThrow_WhenNotFound() {
        when(calendarEventRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> calendarService.delete(99L))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getMonthEvents_ShouldMergeCalendarEventsAndTasks() {
        testEvent.setStartDatetime(LocalDateTime.of(2026, 5, 15, 10, 0));
        when(calendarEventRepository.findByDateRange(any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(List.of(testEvent));

        Task task = Task.builder()
            .id(5L)
            .title("File documents")
            .status(TaskStatus.TODO)
            .dueDate(LocalDate.of(2026, 5, 20))
            .caseEntity(testCase)
            .build();
        when(taskRepository.findUpcoming(any(LocalDate.class), any(LocalDate.class)))
            .thenReturn(List.of(task));

        List<CalendarDayEvent> events = calendarService.getMonthEvents(2026, 5);

        assertThat(events).hasSize(2);
        assertThat(events.stream().map(CalendarDayEvent::source))
            .contains("CALENDAR_EVENT", "TASK");
    }

    @Test
    void getMonthEvents_ShouldExcludeDoneAndCancelledTasks() {
        when(calendarEventRepository.findByDateRange(any(), any()))
            .thenReturn(List.of());

        Task doneTask = Task.builder()
            .id(2L)
            .title("Done task")
            .status(TaskStatus.DONE)
            .dueDate(LocalDate.of(2026, 5, 10))
            .build();
        when(taskRepository.findUpcoming(any(LocalDate.class), any(LocalDate.class)))
            .thenReturn(List.of(doneTask));

        List<CalendarDayEvent> events = calendarService.getMonthEvents(2026, 5);

        assertThat(events).isEmpty();
    }
}
