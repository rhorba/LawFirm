package com.lawfirm.presentation.controller;

import com.lawfirm.application.dto.request.TaskCommentRequest;
import com.lawfirm.application.dto.request.TaskRequest;
import com.lawfirm.application.dto.response.TaskCommentResponse;
import com.lawfirm.application.dto.response.TaskResponse;
import com.lawfirm.application.dto.response.UpcomingTaskResponse;
import com.lawfirm.application.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Tasks", description = "Task and deadline management")
public class TaskController {

    private final TaskService taskService;

    // ── Per-case tasks ────────────────────────────────────────────────────────

    @GetMapping("/api/cases/{caseId}/tasks")
    @PreAuthorize("hasPermission(null, 'TASK_READ')")
    @Operation(summary = "List tasks for a case")
    public ResponseEntity<List<TaskResponse>> listByCaseId(@PathVariable Long caseId) {
        return ResponseEntity.ok(taskService.findByCaseId(caseId));
    }

    @PostMapping("/api/cases/{caseId}/tasks")
    @PreAuthorize("hasPermission(null, 'TASK_CREATE')")
    @Operation(summary = "Create a task on a case")
    public ResponseEntity<TaskResponse> create(
            @PathVariable Long caseId,
            @Valid @RequestBody TaskRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(taskService.create(caseId, request));
    }

    // ── Individual task operations ────────────────────────────────────────────

    @GetMapping("/api/tasks/{id}")
    @PreAuthorize("hasPermission(null, 'TASK_READ')")
    @Operation(summary = "Get task by id")
    public ResponseEntity<TaskResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(taskService.findById(id));
    }

    @PutMapping("/api/tasks/{id}")
    @PreAuthorize("hasPermission(null, 'TASK_UPDATE')")
    @Operation(summary = "Update a task")
    public ResponseEntity<TaskResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody TaskRequest request) {
        return ResponseEntity.ok(taskService.update(id, request));
    }

    @DeleteMapping("/api/tasks/{id}")
    @PreAuthorize("hasPermission(null, 'TASK_DELETE')")
    @Operation(summary = "Delete a task")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        taskService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ── Upcoming deadlines ────────────────────────────────────────────────────

    @GetMapping("/api/tasks/upcoming")
    @PreAuthorize("hasPermission(null, 'TASK_READ')")
    @Operation(summary = "List upcoming tasks (default: next 7 days)")
    public ResponseEntity<List<UpcomingTaskResponse>> upcoming(
            @RequestParam(defaultValue = "7") int days) {
        return ResponseEntity.ok(taskService.findUpcoming(days));
    }

    // ── Comments ──────────────────────────────────────────────────────────────

    @GetMapping("/api/tasks/{taskId}/comments")
    @PreAuthorize("hasPermission(null, 'TASK_READ')")
    @Operation(summary = "List comments on a task")
    public ResponseEntity<List<TaskCommentResponse>> listComments(@PathVariable Long taskId) {
        return ResponseEntity.ok(taskService.findCommentsByTaskId(taskId));
    }

    @PostMapping("/api/tasks/{taskId}/comments")
    @PreAuthorize("hasPermission(null, 'TASK_CREATE')")
    @Operation(summary = "Add a comment to a task")
    public ResponseEntity<TaskCommentResponse> addComment(
            @PathVariable Long taskId,
            @Valid @RequestBody TaskCommentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(taskService.addComment(taskId, request));
    }

    @DeleteMapping("/api/tasks/comments/{commentId}")
    @PreAuthorize("hasPermission(null, 'TASK_DELETE')")
    @Operation(summary = "Delete a task comment")
    public ResponseEntity<Void> deleteComment(@PathVariable Long commentId) {
        taskService.deleteComment(commentId);
        return ResponseEntity.noContent().build();
    }
}
