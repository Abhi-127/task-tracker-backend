package com.tasktracker.backend.controller;

import com.tasktracker.backend.dto.AssignTaskRequest;
import com.tasktracker.backend.dto.TaskRequest;
import com.tasktracker.backend.dto.TaskResponse;
import com.tasktracker.backend.exception.ApiException;
import com.tasktracker.backend.model.Task;
import com.tasktracker.backend.model.TaskStatus;
import com.tasktracker.backend.model.Team;
import com.tasktracker.backend.model.User;
import com.tasktracker.backend.repository.TaskRepository;
import com.tasktracker.backend.repository.TeamRepository;
import com.tasktracker.backend.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final TeamRepository teamRepository;

    public TaskController(
            TaskRepository taskRepository,
            UserRepository userRepository,
            TeamRepository teamRepository
    ) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.teamRepository = teamRepository;
    }

    @PostMapping
    @Transactional
    public ResponseEntity<TaskResponse> createTask(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody TaskRequest request
    ) {
        Task task = new Task();
        task.setTitle(request.title().trim());
        task.setDescription(request.description());
        task.setDueDate(request.dueDate());
        task.setStatus(request.status() != null ? request.status() : TaskStatus.OPEN);
        task.setCreatedBy(currentUser);

        if (request.teamId() != null) {
            Team team = teamRepository.findById(request.teamId())
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Team not found"));
            task.setTeam(team);
        }

        if (request.assignedToId() != null) {
            User assignee = userRepository.findById(request.assignedToId())
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Assignee user not found"));
            task.setAssignedTo(assignee);
        } else {
            task.setAssignedTo(currentUser);
        }

        Task saved = taskRepository.save(task);
        return ResponseEntity.status(HttpStatus.CREATED).body(TaskResponse.from(saved));
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<TaskResponse> getTasks(
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) Long teamId,
            @RequestParam(required = false) Long assignedToId,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        Sort sort = buildSort(sortBy, sortDir);
        String keyword = (q == null) ? "" : q.trim();
        return taskRepository.searchTasks(status, teamId, assignedToId, keyword, sort)
                .stream()
                .map(TaskResponse::from)
                .toList();
    }

    @GetMapping("/my")
    @Transactional(readOnly = true)
    public List<TaskResponse> getMyTasks(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(defaultValue = "dueDate") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir
    ) {
        Sort sort = buildSort(sortBy, sortDir);
        return taskRepository.searchTasks(status, null, currentUser.getId(), "", sort)
                .stream()
                .map(TaskResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<TaskResponse> getTaskById(@PathVariable Long id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Task not found"));
        return ResponseEntity.ok(TaskResponse.from(task));
    }

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<TaskResponse> updateTask(
            @PathVariable Long id,
            @Valid @RequestBody TaskRequest request
    ) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Task not found"));

        task.setTitle(request.title().trim());
        task.setDescription(request.description());
        task.setDueDate(request.dueDate());
        if (request.status() != null) {
            task.setStatus(request.status());
        }
        if (request.teamId() != null) {
            Team team = teamRepository.findById(request.teamId())
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Team not found"));
            task.setTeam(team);
        }
        if (request.assignedToId() != null) {
            User assignee = userRepository.findById(request.assignedToId())
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Assignee user not found"));
            task.setAssignedTo(assignee);
        }

        Task updated = taskRepository.save(task);
        return ResponseEntity.ok(TaskResponse.from(updated));
    }

    @PatchMapping("/{id}/complete")
    @Transactional
    public ResponseEntity<TaskResponse> markTaskCompleted(@PathVariable Long id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Task not found"));
        task.setStatus(TaskStatus.COMPLETED);
        Task updated = taskRepository.save(task);
        return ResponseEntity.ok(TaskResponse.from(updated));
    }

    @PatchMapping("/{id}/assign")
    @Transactional
    public ResponseEntity<TaskResponse> assignTask(
            @PathVariable Long id,
            @Valid @RequestBody AssignTaskRequest request
    ) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Task not found"));
        User assignee = userRepository.findById(request.assigneeId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Assignee user not found"));

        if (task.getTeam() != null) {
            boolean isMember = task.getTeam().getMembers().stream()
                    .anyMatch(member -> member.getId().equals(assignee.getId()));
            if (!isMember) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Assignee must be a member of the task's team");
            }
        }

        task.setAssignedTo(assignee);
        Task updated = taskRepository.save(task);
        return ResponseEntity.ok(TaskResponse.from(updated));
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Object> deleteTask(@PathVariable Long id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Task not found"));
        taskRepository.delete(task);
        return ResponseEntity.ok(Map.of("message", "Task deleted successfully"));
    }

    private Sort buildSort(String sortBy, String sortDir) {
        String field = switch (sortBy) {
            case "dueDate", "title", "status", "createdAt", "updatedAt" -> sortBy;
            default -> "createdAt";
        };
        return "asc".equalsIgnoreCase(sortDir)
                ? Sort.by(field).ascending()
                : Sort.by(field).descending();
    }
}
