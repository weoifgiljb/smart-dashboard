package com.selfdiscipline.controller;

import com.selfdiscipline.dto.BulkStatusRequest;
import com.selfdiscipline.dto.MessageResponse;
import com.selfdiscipline.dto.TaskCreateRequest;
import com.selfdiscipline.dto.TaskPatchRequest;
import com.selfdiscipline.model.Task;
import com.selfdiscipline.model.TaskHistory;
import com.selfdiscipline.model.TaskShare;
import com.selfdiscipline.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping
    public ResponseEntity<Task> create(@Valid @RequestBody TaskCreateRequest req, Authentication auth) {
        return ResponseEntity.ok(taskService.createFromRequest(Objects.requireNonNull(auth.getName()), req));
    }

    @GetMapping
    public ResponseEntity<List<Task>> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) String tag,
            Authentication auth
    ) {
        List<Task> tasks = taskService.listTasks(Objects.requireNonNull(auth.getName()), Map.of(
                "q", q == null ? "" : q,
                "status", status == null ? "" : status,
                "priority", priority == null ? "" : priority,
                "tag", tag == null ? "" : tag
        ));
        return ResponseEntity.ok(tasks);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Task> detail(@PathVariable @NonNull String id, Authentication auth) {
        return ResponseEntity.ok(taskService.getTask(Objects.requireNonNull(auth.getName()), id));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Task> update(@PathVariable @NonNull String id,
                                       @RequestBody TaskPatchRequest patch,
                                       Authentication auth) {
        return ResponseEntity.ok(taskService.updateFromRequest(Objects.requireNonNull(auth.getName()), id, patch));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<MessageResponse> delete(@PathVariable @NonNull String id, Authentication auth) {
        taskService.deleteTask(Objects.requireNonNull(auth.getName()), id);
        return ResponseEntity.ok(new MessageResponse("删除成功"));
    }

    @PostMapping("/{id}/subtasks")
    public ResponseEntity<Task> createSubtask(@PathVariable @NonNull String id,
                                              @Valid @RequestBody TaskCreateRequest req,
                                              Authentication auth) {
        Task sub = new Task();
        sub.setTitle(req.getTitle());
        sub.setDescription(req.getDescription());
        sub.setStatus(req.getStatus());
        sub.setPriority(req.getPriority());
        sub.setTags(req.getTags());
        return ResponseEntity.ok(taskService.createSubtask(Objects.requireNonNull(auth.getName()), id, sub));
    }

    @GetMapping("/{id}/subtasks")
    public ResponseEntity<List<Task>> subtasks(@PathVariable @NonNull String id, Authentication auth) {
        return ResponseEntity.ok(taskService.getSubtasks(Objects.requireNonNull(auth.getName()), id));
    }

    @PostMapping("/{id}/dependencies")
    public ResponseEntity<Task> addDep(@PathVariable @NonNull String id,
                                       @RequestParam @NonNull String depId,
                                       Authentication auth) {
        return ResponseEntity.ok(taskService.addDependency(Objects.requireNonNull(auth.getName()), id, depId));
    }

    @DeleteMapping("/{id}/dependencies/{depId}")
    public ResponseEntity<Task> removeDep(@PathVariable @NonNull String id,
                                          @PathVariable @NonNull String depId,
                                          Authentication auth) {
        return ResponseEntity.ok(taskService.removeDependency(Objects.requireNonNull(auth.getName()), id, depId));
    }

    @PostMapping("/bulk/status")
    public ResponseEntity<List<Task>> bulkStatus(@Valid @RequestBody BulkStatusRequest body, Authentication auth) {
        String username = Objects.requireNonNull(auth.getName());
        TaskPatchRequest patch = new TaskPatchRequest();
        patch.setStatus(body.getStatus());
        return ResponseEntity.ok(body.getIds().stream()
                .map(id -> taskService.updateFromRequest(username, id, patch))
                .toList());
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<List<TaskHistory>> history(@PathVariable @NonNull String id, Authentication auth) {
        return ResponseEntity.ok(taskService.getHistory(Objects.requireNonNull(auth.getName()), id));
    }

    @GetMapping("/{id}/share")
    public ResponseEntity<List<TaskShare>> shares(@PathVariable @NonNull String id, Authentication auth) {
        return ResponseEntity.ok(taskService.getShares(Objects.requireNonNull(auth.getName()), id));
    }

    @PostMapping("/{id}/share")
    public ResponseEntity<TaskShare> setShare(@PathVariable @NonNull String id,
                                              @RequestParam @NonNull String userId,
                                              @RequestParam String role,
                                              Authentication auth) {
        return ResponseEntity.ok(taskService.setShare(Objects.requireNonNull(auth.getName()), id, userId, role));
    }

    @DeleteMapping("/{id}/share/{userId}")
    public ResponseEntity<MessageResponse> removeShare(@PathVariable @NonNull String id,
                                                       @PathVariable @NonNull String userId,
                                                       Authentication auth) {
        taskService.removeShare(Objects.requireNonNull(auth.getName()), id, userId);
        return ResponseEntity.ok(new MessageResponse("已移除分享"));
    }

    @GetMapping("/reminders/soon")
    public ResponseEntity<List<Task>> reminders(@RequestParam(defaultValue = "60") int windowMinutes, Authentication auth) {
        return ResponseEntity.ok(taskService.getRemindersSoon(Objects.requireNonNull(auth.getName()), windowMinutes));
    }

    @GetMapping("/aggregate/kanban")
    public ResponseEntity<List<Map<String, Object>>> kanban(Authentication auth) {
        return ResponseEntity.ok(taskService.getKanban(Objects.requireNonNull(auth.getName())));
    }

    @GetMapping("/aggregate/stats")
    public ResponseEntity<Map<String, Object>> stats(Authentication auth) {
        return ResponseEntity.ok(taskService.getStats(Objects.requireNonNull(auth.getName())));
    }

    @GetMapping("/aggregate/gantt")
    public ResponseEntity<List<Task>> gantt(@RequestParam @NonNull String start,
                                            @RequestParam @NonNull String end,
                                            Authentication auth) {
        LocalDateTime s = LocalDateTime.parse(start);
        LocalDateTime e = LocalDateTime.parse(end);
        return ResponseEntity.ok(taskService.getGantt(Objects.requireNonNull(auth.getName()), s, e));
    }
}
