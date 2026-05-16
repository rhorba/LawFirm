package com.lawfirm.application.service;

import com.lawfirm.application.dto.request.TaskCommentRequest;
import com.lawfirm.application.dto.request.TaskRequest;
import com.lawfirm.application.dto.response.TaskCommentResponse;
import com.lawfirm.application.dto.response.TaskResponse;
import com.lawfirm.application.mapper.TaskCommentMapper;
import com.lawfirm.application.mapper.TaskMapper;
import com.lawfirm.domain.model.Case;
import com.lawfirm.domain.model.Lawyer;
import com.lawfirm.domain.model.Task;
import com.lawfirm.domain.model.Task.TaskPriority;
import com.lawfirm.domain.model.Task.TaskStatus;
import com.lawfirm.domain.model.TaskComment;
import com.lawfirm.domain.model.User;
import com.lawfirm.domain.repository.CaseRepository;
import com.lawfirm.domain.repository.LawyerRepository;
import com.lawfirm.domain.repository.TaskCommentRepository;
import com.lawfirm.domain.repository.TaskRepository;
import com.lawfirm.domain.repository.UserRepository;
import com.lawfirm.presentation.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock private TaskRepository taskRepository;
    @Mock private TaskCommentRepository taskCommentRepository;
    @Mock private CaseRepository caseRepository;
    @Mock private LawyerRepository lawyerRepository;
    @Mock private UserRepository userRepository;
    @Mock private TaskMapper taskMapper;
    @Mock private TaskCommentMapper taskCommentMapper;

    @InjectMocks
    private TaskService taskService;

    private Task testTask;
    private TaskResponse testTaskResponse;
    private Case testCase;
    private User testUser;

    @BeforeEach
    void setUp() {
        testCase = new Case();
        testCase.setId(10L);

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("admin");

        testTask = Task.builder()
            .id(1L)
            .title("Draft legal brief")
            .status(TaskStatus.TODO)
            .priority(TaskPriority.HIGH)
            .dueDate(LocalDate.now().plusDays(7))
            .caseEntity(testCase)
            .createdBy(testUser)
            .build();

        testTaskResponse = mock(TaskResponse.class);
        lenient().when(testTaskResponse.id()).thenReturn(1L);
        lenient().when(testTaskResponse.title()).thenReturn("Draft legal brief");
        lenient().when(testTaskResponse.status()).thenReturn(TaskStatus.TODO);
    }

    private void mockSecurityContext() {
        SecurityContext ctx = mock(SecurityContext.class);
        Authentication auth = mock(Authentication.class);
        lenient().when(ctx.getAuthentication()).thenReturn(auth);
        lenient().when(auth.getName()).thenReturn("admin");
        SecurityContextHolder.setContext(ctx);
        lenient().when(userRepository.findByUsernameAndDeletedAtIsNull("admin"))
            .thenReturn(Optional.of(testUser));
    }

    @Test
    void findById_ShouldReturnTask_WhenExists() {
        when(taskRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(testTask));
        when(taskMapper.toResponse(testTask)).thenReturn(testTaskResponse);

        TaskResponse result = taskService.findById(1L);

        assertThat(result).isNotNull();
        assertThat(result.title()).isEqualTo("Draft legal brief");
    }

    @Test
    void findById_ShouldThrow_WhenNotFound() {
        when(taskRepository.findByIdWithDetails(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.findById(99L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Task not found");
    }

    @Test
    void findByCaseId_ShouldReturnTaskList() {
        when(taskRepository.findByCaseIdWithDetails(10L)).thenReturn(List.of(testTask));
        when(taskMapper.toResponseList(anyList())).thenReturn(List.of(testTaskResponse));

        List<TaskResponse> result = taskService.findByCaseId(10L);

        assertThat(result).hasSize(1);
    }

    @Test
    void create_ShouldSaveTask_WithDefaultStatusTodo() {
        mockSecurityContext();
        TaskRequest request = mock(TaskRequest.class);
        when(request.title()).thenReturn("Draft legal brief");
        when(request.dueDate()).thenReturn(LocalDate.now().plusDays(7));
        when(request.status()).thenReturn(null);
        when(request.priority()).thenReturn(TaskPriority.HIGH);
        when(request.assignedLawyerId()).thenReturn(null);
        when(caseRepository.findById(10L)).thenReturn(Optional.of(testCase));
        when(taskRepository.save(any(Task.class))).thenReturn(testTask);
        when(taskMapper.toResponse(testTask)).thenReturn(testTaskResponse);

        TaskResponse result = taskService.create(10L, request);

        assertThat(result).isNotNull();
        verify(taskRepository).save(any(Task.class));
    }

    @Test
    void create_ShouldThrow_WhenCaseNotFound() {
        mockSecurityContext();
        TaskRequest request = mock(TaskRequest.class);
        when(caseRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.create(99L, request))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Case not found");
    }

    @Test
    void create_ShouldAssignLawyer_WhenLawyerIdProvided() {
        mockSecurityContext();
        Lawyer lawyer = new Lawyer();
        lawyer.setId(5L);
        TaskRequest request = mock(TaskRequest.class);
        when(request.title()).thenReturn("Task with lawyer");
        when(request.status()).thenReturn(TaskStatus.TODO);
        when(request.assignedLawyerId()).thenReturn(5L);
        when(caseRepository.findById(10L)).thenReturn(Optional.of(testCase));
        when(lawyerRepository.findById(5L)).thenReturn(Optional.of(lawyer));
        when(taskRepository.save(any(Task.class))).thenReturn(testTask);
        when(taskMapper.toResponse(testTask)).thenReturn(testTaskResponse);

        taskService.create(10L, request);

        verify(lawyerRepository).findById(5L);
    }

    @Test
    void delete_ShouldCallRepository_WhenExists() {
        when(taskRepository.existsById(1L)).thenReturn(true);

        taskService.delete(1L);

        verify(taskRepository).deleteById(1L);
    }

    @Test
    void delete_ShouldThrow_WhenNotFound() {
        when(taskRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> taskService.delete(99L))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void addComment_ShouldSaveComment() {
        mockSecurityContext();
        TaskCommentRequest request = mock(TaskCommentRequest.class);
        when(request.content()).thenReturn("This needs review");
        when(taskRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(testTask));
        TaskComment comment = TaskComment.builder().id(1L).content("This needs review").build();
        when(taskCommentRepository.save(any(TaskComment.class))).thenReturn(comment);
        TaskCommentResponse commentResponse = mock(TaskCommentResponse.class);
        when(commentResponse.content()).thenReturn("This needs review");
        when(taskCommentMapper.toResponse(comment)).thenReturn(commentResponse);

        TaskCommentResponse result = taskService.addComment(1L, request);

        assertThat(result.content()).isEqualTo("This needs review");
    }

    @Test
    void deleteComment_ShouldThrow_WhenNotFound() {
        when(taskCommentRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> taskService.deleteComment(99L))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void findUpcoming_ShouldReturnTasksDueSoon() {
        when(taskRepository.findUpcoming(any(LocalDate.class), any(LocalDate.class)))
            .thenReturn(List.of(testTask));
        when(taskMapper.toUpcomingList(anyList())).thenReturn(List.of());

        taskService.findUpcoming(7);

        verify(taskRepository).findUpcoming(any(LocalDate.class), any(LocalDate.class));
    }
}
