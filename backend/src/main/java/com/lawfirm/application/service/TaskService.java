package com.lawfirm.application.service;

import com.lawfirm.application.dto.request.TaskCommentRequest;
import com.lawfirm.application.dto.request.TaskRequest;
import com.lawfirm.application.dto.response.TaskCommentResponse;
import com.lawfirm.application.dto.response.TaskResponse;
import com.lawfirm.application.dto.response.UpcomingTaskResponse;
import com.lawfirm.application.mapper.TaskCommentMapper;
import com.lawfirm.application.mapper.TaskMapper;
import com.lawfirm.domain.model.Task;
import com.lawfirm.domain.model.Task.TaskStatus;
import com.lawfirm.domain.model.TaskComment;
import com.lawfirm.domain.repository.CaseRepository;
import com.lawfirm.domain.repository.LawyerRepository;
import com.lawfirm.domain.repository.TaskCommentRepository;
import com.lawfirm.domain.repository.TaskRepository;
import com.lawfirm.domain.repository.UserRepository;
import com.lawfirm.presentation.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TaskService {

    private final TaskRepository taskRepository;
    private final TaskCommentRepository taskCommentRepository;
    private final CaseRepository caseRepository;
    private final LawyerRepository lawyerRepository;
    private final UserRepository userRepository;
    private final TaskMapper taskMapper;
    private final TaskCommentMapper taskCommentMapper;

    // ── Tasks ─────────────────────────────────────────────────────────────────

    public List<TaskResponse> findByCaseId(Long caseId) {
        return taskMapper.toResponseList(taskRepository.findByCaseIdWithDetails(caseId));
    }

    public TaskResponse findById(Long id) {
        return taskMapper.toResponse(getTaskOrThrow(id));
    }

    public List<UpcomingTaskResponse> findUpcoming(int days) {
        LocalDate from = LocalDate.now();
        LocalDate to = from.plusDays(days);
        return taskMapper.toUpcomingList(taskRepository.findUpcoming(from, to));
    }

    @Transactional
    public TaskResponse create(Long caseId, TaskRequest request) {
        var caseEntity = caseRepository.findById(caseId)
            .orElseThrow(() -> new ResourceNotFoundException("Case not found: " + caseId));

        var task = Task.builder()
            .title(request.title())
            .description(request.description())
            .dueDate(request.dueDate())
            .status(request.status() != null ? request.status() : TaskStatus.TODO)
            .priority(request.priority())
            .caseEntity(caseEntity)
            .createdBy(getCurrentUser())
            .build();

        if (request.assignedLawyerId() != null) {
            task.setAssignedLawyer(lawyerRepository.findById(request.assignedLawyerId())
                .orElseThrow(() -> new ResourceNotFoundException("Lawyer not found: " + request.assignedLawyerId())));
        }

        return taskMapper.toResponse(taskRepository.save(task));
    }

    @Transactional
    public TaskResponse update(Long id, TaskRequest request) {
        var task = getTaskOrThrow(id);

        task.setTitle(request.title());
        task.setDescription(request.description());
        task.setDueDate(request.dueDate());
        task.setStatus(request.status());
        task.setPriority(request.priority());

        if (request.assignedLawyerId() != null) {
            task.setAssignedLawyer(lawyerRepository.findById(request.assignedLawyerId())
                .orElseThrow(() -> new ResourceNotFoundException("Lawyer not found: " + request.assignedLawyerId())));
        } else {
            task.setAssignedLawyer(null);
        }

        return taskMapper.toResponse(taskRepository.save(task));
    }

    @Transactional
    public void delete(Long id) {
        if (!taskRepository.existsById(id)) {
            throw new ResourceNotFoundException("Task not found: " + id);
        }
        taskRepository.deleteById(id);
    }

    // ── Comments ──────────────────────────────────────────────────────────────

    public List<TaskCommentResponse> findCommentsByTaskId(Long taskId) {
        return taskCommentMapper.toResponseList(
            taskCommentRepository.findByTaskIdWithAuthor(taskId));
    }

    @Transactional
    public TaskCommentResponse addComment(Long taskId, TaskCommentRequest request) {
        var task = getTaskOrThrow(taskId);
        var comment = TaskComment.builder()
            .task(task)
            .author(getCurrentUser())
            .content(request.content())
            .build();
        return taskCommentMapper.toResponse(taskCommentRepository.save(comment));
    }

    @Transactional
    public void deleteComment(Long commentId) {
        if (!taskCommentRepository.existsById(commentId)) {
            throw new ResourceNotFoundException("TaskComment not found: " + commentId);
        }
        taskCommentRepository.deleteById(commentId);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Task getTaskOrThrow(Long id) {
        return taskRepository.findByIdWithDetails(id)
            .orElseThrow(() -> new ResourceNotFoundException("Task not found: " + id));
    }

    private com.lawfirm.domain.model.User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsernameAndDeletedAtIsNull(username)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
    }
}
