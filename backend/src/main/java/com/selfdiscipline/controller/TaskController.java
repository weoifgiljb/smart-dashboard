package com.selfdiscipline.controller;

import com.selfdiscipline.model.Task;
import com.selfdiscipline.model.TaskHistory;
import com.selfdiscipline.model.TaskShare;
import com.selfdiscipline.service.TaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.lang.NonNull;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ArrayList;

@RestController
@RequestMapping("/tasks")
public class TaskController {

    @Autowired
    private TaskService taskService;

    @PostMapping
    public ResponseEntity<Task> create(@RequestBody Task req, Authentication auth) {
        Task task = taskService.createTask(Objects.requireNonNull(auth.getName()), req);
        return ResponseEntity.ok(task);
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
        return ResponseEntity.ok(taskService.getTask(Objects.requireNonNull(auth.getName()), Objects.requireNonNull(id)));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Task> update(@PathVariable @NonNull String id, @RequestBody Map<String, Object> patch, Authentication auth) {
        return ResponseEntity.ok(taskService.updateTask(Objects.requireNonNull(auth.getName()), Objects.requireNonNull(id), patch));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> delete(@PathVariable @NonNull String id, Authentication auth) {
        taskService.deleteTask(Objects.requireNonNull(auth.getName()), Objects.requireNonNull(id));
        return ResponseEntity.ok(Map.of("message", "删除成功"));
    }

    @PostMapping("/{id}/subtasks")
    public ResponseEntity<Task> createSubtask(@PathVariable @NonNull String id, @RequestBody Task req, Authentication auth) {
        return ResponseEntity.ok(taskService.createSubtask(Objects.requireNonNull(auth.getName()), Objects.requireNonNull(id), req));
    }

    @GetMapping("/{id}/subtasks")
    public ResponseEntity<List<Task>> subtasks(@PathVariable @NonNull String id, Authentication auth) {
        return ResponseEntity.ok(taskService.getSubtasks(Objects.requireNonNull(auth.getName()), Objects.requireNonNull(id)));
    }

    @PostMapping("/{id}/dependencies")
    public ResponseEntity<Task> addDep(@PathVariable @NonNull String id, @RequestParam @NonNull String depId, Authentication auth) {
        return ResponseEntity.ok(taskService.addDependency(Objects.requireNonNull(auth.getName()), Objects.requireNonNull(id), Objects.requireNonNull(depId)));
    }

    @DeleteMapping("/{id}/dependencies/{depId}")
    public ResponseEntity<Task> removeDep(@PathVariable @NonNull String id, @PathVariable @NonNull String depId, Authentication auth) {
        return ResponseEntity.ok(taskService.removeDependency(Objects.requireNonNull(auth.getName()), Objects.requireNonNull(id), Objects.requireNonNull(depId)));
    }

    @PostMapping("/bulk/status")
    public ResponseEntity<List<Task>> bulkStatus(@RequestBody Map<String, Object> body, Authentication auth) {
        List<String> ids = new ArrayList<>();
        Object idsObj = body.get("ids");
        if (idsObj instanceof List<?> raw) {
            ids = raw.stream().filter(Objects::nonNull).map(Object::toString).toList();
        }
        String status = Objects.toString(body.get("status"), "");
        return ResponseEntity.ok(ids.stream().map(id -> taskService.updateTask(Objects.requireNonNull(auth.getName()), Objects.requireNonNull(id), Map.of("status", status))).toList());
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<List<TaskHistory>> history(@PathVariable @NonNull String id, Authentication auth) {
        return ResponseEntity.ok(taskService.getHistory(Objects.requireNonNull(auth.getName()), Objects.requireNonNull(id)));
    }

    @GetMapping("/{id}/share")
    public ResponseEntity<List<TaskShare>> shares(@PathVariable @NonNull String id, Authentication auth) {
        return ResponseEntity.ok(taskService.getShares(Objects.requireNonNull(auth.getName()), Objects.requireNonNull(id)));
    }

    @PostMapping("/{id}/share")
    public ResponseEntity<TaskShare> setShare(@PathVariable @NonNull String id, @RequestParam @NonNull String userId, @RequestParam String role, Authentication auth) {
        return ResponseEntity.ok(taskService.setShare(Objects.requireNonNull(auth.getName()), Objects.requireNonNull(id), Objects.requireNonNull(userId), role));
    }

    @DeleteMapping("/{id}/share/{userId}")
    public ResponseEntity<Map<String, String>> removeShare(@PathVariable @NonNull String id, @PathVariable @NonNull String userId, Authentication auth) {
        taskService.removeShare(Objects.requireNonNull(auth.getName()), Objects.requireNonNull(id), Objects.requireNonNull(userId));
        return ResponseEntity.ok(Map.of("message", "已移除分享"));
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
    public ResponseEntity<List<Task>> gantt(@RequestParam @NonNull String start, @RequestParam @NonNull String end, Authentication auth) {
        LocalDateTime s = LocalDateTime.parse(Objects.requireNonNull(start));
        LocalDateTime e = LocalDateTime.parse(Objects.requireNonNull(end));
        return ResponseEntity.ok(taskService.getGantt(Objects.requireNonNull(auth.getName()), s, e));
    }
}


