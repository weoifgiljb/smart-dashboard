package com.selfdiscipline.service;

import com.selfdiscipline.model.Task;
import com.selfdiscipline.model.TaskHistory;
import com.selfdiscipline.model.TaskShare;
import com.selfdiscipline.repository.TaskHistoryRepository;
import com.selfdiscipline.repository.TaskRepository;
import com.selfdiscipline.repository.TaskShareRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TaskService {

    @Autowired
    private TaskRepository taskRepository;
    @Autowired
    private TaskHistoryRepository historyRepository;
    @Autowired
    private TaskShareRepository shareRepository;

    public Task createTask(@NonNull String userId, Task task) {
        task.setOwnerUserId(userId);
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(task.getCreatedAt());
        Task saved = taskRepository.save(task);
        recordHistory(saved.getId(), userId, "CREATE", null, null, null);
        return saved;
    }

    public Task getTask(@NonNull String userId, @NonNull String taskId) {
        Task t = taskRepository.findById(taskId).orElseThrow(() -> new RuntimeException("任务不存在"));
        assertCanRead(userId, t);
        return t;
    }

    public List<Task> listTasks(@NonNull String userId, Map<String, Object> filters) {
        // 简化：先取我拥有的，再合并我被共享的
        List<Task> mine = taskRepository.findByOwnerUserId(userId);
        List<TaskShare> shares = shareRepository.findByTargetUserId(userId);
        Set<String> sharedTaskIds = shares.stream().map(TaskShare::getTaskId).collect(Collectors.toSet());
        List<Task> shared = sharedTaskIds.isEmpty() ? List.of() :
                taskRepository.findAllById(sharedTaskIds);
        List<Task> all = new ArrayList<>();
        all.addAll(mine);
        all.addAll(shared);
        // 过滤
        return applyFilters(all, filters);
    }

    private List<Task> applyFilters(List<Task> input, Map<String, Object> filters) {
        if (filters == null || filters.isEmpty()) return input;
        return input.stream().filter(t -> {
            if (filters.containsKey("status")) {
                String s = Objects.toString(filters.get("status"), "");
                if (StringUtils.hasText(s) && !s.equalsIgnoreCase(t.getStatus())) return false;
            }
            if (filters.containsKey("priority")) {
                String p = Objects.toString(filters.get("priority"), "");
                if (StringUtils.hasText(p) && !p.equalsIgnoreCase(t.getPriority())) return false;
            }
            if (filters.containsKey("tag")) {
                String tag = Objects.toString(filters.get("tag"), "");
                if (StringUtils.hasText(tag) && (t.getTags() == null || !t.getTags().contains(tag))) return false;
            }
            if (filters.containsKey("q")) {
                String q = Objects.toString(filters.get("q"), "").toLowerCase();
                if (StringUtils.hasText(q)) {
                    String title = Optional.ofNullable(t.getTitle()).orElse("").toLowerCase();
                    String desc = Optional.ofNullable(t.getDescription()).orElse("").toLowerCase();
                    if (!(title.contains(q) || desc.contains(q))) return false;
                }
            }
            return true;
        }).toList();
    }

    public Task updateTask(@NonNull String userId, @NonNull String taskId, Map<String, Object> patch) {
        Task t = taskRepository.findById(Objects.requireNonNull(taskId)).orElseThrow(() -> new RuntimeException("任务不存在"));
        assertCanEdit(userId, t);
        Map<String, Object> before = snapshot(t);
        applyPatch(t, patch);
        t.setUpdatedAt(LocalDateTime.now());
        Task saved = taskRepository.save(t);
        recordDiffHistory(taskId, userId, before, snapshot(saved));
        // 完成时生成下一实例（简化：仅当存在 recurrenceRule）
        if ("done".equalsIgnoreCase(saved.getStatus()) && StringUtils.hasText(saved.getRecurrenceRule())) {
            generateNextOccurrence(saved);
        }
        return saved;
    }

    public void deleteTask(@NonNull String userId, @NonNull String taskId) {
        Task t = taskRepository.findById(Objects.requireNonNull(taskId)).orElseThrow(() -> new RuntimeException("任务不存在"));
        assertCanEdit(userId, t);
        taskRepository.deleteById(Objects.requireNonNull(taskId));
        recordHistory(taskId, userId, "DELETE", null, null, null);
    }

    public Task createSubtask(@NonNull String userId, @NonNull String parentId, Task sub) {
        Task parent = taskRepository.findById(Objects.requireNonNull(parentId)).orElseThrow(() -> new RuntimeException("父任务不存在"));
        assertCanEdit(userId, parent);
        sub.setParentId(parentId);
        sub.setSeriesId(parent.getSeriesId());
        return createTask(userId, sub);
    }

    public List<Task> getSubtasks(@NonNull String userId, @NonNull String taskId) {
        getTask(userId, taskId);
        return taskRepository.findAll().stream().filter(x -> taskId.equals(x.getParentId())).toList();
    }

    public Task addDependency(@NonNull String userId, @NonNull String taskId, @NonNull String depId) {
        Task t = taskRepository.findById(Objects.requireNonNull(taskId)).orElseThrow(() -> new RuntimeException("任务不存在"));
        Task dep = taskRepository.findById(Objects.requireNonNull(depId)).orElseThrow(() -> new RuntimeException("依赖任务不存在"));
        assertCanEdit(userId, t);
        if (t.getDependencyIds() == null) t.setDependencyIds(new ArrayList<>());
        if (!t.getDependencyIds().contains(dep.getId())) t.getDependencyIds().add(dep.getId());
        return taskRepository.save(t);
    }

    public Task removeDependency(@NonNull String userId, @NonNull String taskId, @NonNull String depId) {
        Task t = taskRepository.findById(Objects.requireNonNull(taskId)).orElseThrow(() -> new RuntimeException("任务不存在"));
        assertCanEdit(userId, t);
        if (t.getDependencyIds() != null) t.getDependencyIds().remove(depId);
        return taskRepository.save(t);
    }

    public List<TaskHistory> getHistory(@NonNull String userId, @NonNull String taskId) {
        Task t = taskRepository.findById(Objects.requireNonNull(taskId)).orElseThrow(() -> new RuntimeException("任务不存在"));
        assertCanRead(userId, t);
        return historyRepository.findByTaskIdOrderByCreatedAtAsc(taskId);
    }

    public List<Map<String, Object>> getKanban(@NonNull String userId) {
        Map<String, Object> filters = new HashMap<>();
        List<Task> tasks = listTasks(userId, filters);
        return tasks.stream().collect(Collectors.groupingBy(Task::getStatus)).entrySet().stream()
                .map(e -> Map.of("status", e.getKey(), "items", e.getValue()))
                .toList();
    }

    public Map<String, Object> getStats(@NonNull String userId) {
        List<Task> tasks = listTasks(userId, Map.of());
        Map<String, Long> byStatus = tasks.stream().collect(Collectors.groupingBy(Task::getStatus, Collectors.counting()));
        Map<String, Long> byPriority = tasks.stream().collect(Collectors.groupingBy(Task::getPriority, Collectors.counting()));
        long overdue = tasks.stream().filter(t -> t.getDueDate() != null && t.getDueDate().isBefore(LocalDateTime.now()) && !"done".equalsIgnoreCase(t.getStatus())).count();
        return Map.of("byStatus", byStatus, "byPriority", byPriority, "overdue", overdue);
    }

    public List<Task> getGantt(@NonNull String userId, LocalDateTime start, LocalDateTime end) {
        List<Task> tasks = listTasks(userId, Map.of());
        return tasks.stream().filter(t ->
                (t.getStartDate() != null && t.getDueDate() != null) &&
                        !t.getDueDate().isBefore(start) && !t.getStartDate().isAfter(end)
        ).toList();
    }

    public List<Task> getRemindersSoon(@NonNull String userId, int windowMinutes) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime end = now.plusMinutes(windowMinutes);
        return listTasks(userId, Map.of()).stream()
                .filter(t -> t.getRemindAt() != null && !t.getRemindAt().isBefore(now) && !t.getRemindAt().isAfter(end))
                .toList();
    }

    public List<TaskShare> getShares(@NonNull String userId, @NonNull String taskId) {
        Task t = taskRepository.findById(Objects.requireNonNull(taskId)).orElseThrow(() -> new RuntimeException("任务不存在"));
        assertCanRead(userId, t);
        return shareRepository.findByTaskId(taskId);
    }

    public TaskShare setShare(@NonNull String userId, @NonNull String taskId, @NonNull String targetUserId, String role) {
        Task t = taskRepository.findById(Objects.requireNonNull(taskId)).orElseThrow(() -> new RuntimeException("任务不存在"));
        assertCanEdit(userId, t);
        List<TaskShare> existing = shareRepository.findByTaskId(taskId).stream()
                .filter(s -> Objects.equals(s.getTargetUserId(), targetUserId))
                .toList();
        for (TaskShare s : existing) {
            shareRepository.deleteById(Objects.requireNonNull(s.getId()));
        }
        TaskShare s = new TaskShare();
        s.setTaskId(taskId);
        s.setTargetUserId(targetUserId);
        s.setRole(Objects.equals(role, "EDIT") ? "EDIT" : "VIEW");
        s.setGrantedByUserId(userId);
        TaskShare saved = shareRepository.save(s);
        recordHistory(taskId, userId, "SHARE", "role", null, saved.getRole());
        return saved;
    }

    public void removeShare(@NonNull String userId, @NonNull String taskId, @NonNull String targetUserId) {
        Task t = taskRepository.findById(Objects.requireNonNull(taskId)).orElseThrow(() -> new RuntimeException("任务不存在"));
        assertCanEdit(userId, t);
        List<TaskShare> existing = shareRepository.findByTaskId(taskId);
        for (TaskShare s : existing) {
            if (Objects.equals(s.getTargetUserId(), targetUserId)) {
                shareRepository.deleteById(Objects.requireNonNull(s.getId()));
            }
        }
    }

    private void applyPatch(Task t, Map<String, Object> patch) {
        if (patch == null) return;
        // 依赖阻塞规则：存在未完成依赖则不能置为进行中/完成
        if (patch.containsKey("status")) {
            String newStatus = Objects.toString(patch.get("status"), t.getStatus());
            if (("in_progress".equalsIgnoreCase(newStatus) || "done".equalsIgnoreCase(newStatus))
                    && t.getDependencyIds() != null && !t.getDependencyIds().isEmpty()) {
                List<String> depIds = t.getDependencyIds().stream().filter(Objects::nonNull).toList();
                List<Task> deps = taskRepository.findAllById(Objects.requireNonNull(depIds));
                boolean hasUndone = deps.stream().anyMatch(d -> !"done".equalsIgnoreCase(d.getStatus()));
                if (hasUndone) {
                    throw new RuntimeException("存在未完成依赖，无法开始或完成该任务");
                }
            }
            t.setStatus(newStatus);
        }
        if (patch.containsKey("title")) t.setTitle(Objects.toString(patch.get("title"), t.getTitle()));
        if (patch.containsKey("description")) t.setDescription(Objects.toString(patch.get("description"), t.getDescription()));
        if (patch.containsKey("priority")) t.setPriority(Objects.toString(patch.get("priority"), t.getPriority()));
        if (patch.containsKey("tags")) {
            Object tagsObj = patch.get("tags");
            if (tagsObj instanceof List<?> raw) {
                List<String> tags = raw.stream().filter(Objects::nonNull).map(Object::toString).toList();
                t.setTags(tags);
            }
        }
        if (patch.containsKey("startDate")) t.setStartDate((LocalDateTime) patch.get("startDate"));
        if (patch.containsKey("dueDate")) t.setDueDate((LocalDateTime) patch.get("dueDate"));
        if (patch.containsKey("estimateMinutes")) t.setEstimateMinutes((Integer) patch.getOrDefault("estimateMinutes", t.getEstimateMinutes()));
        if (patch.containsKey("actualMinutes")) t.setActualMinutes((Integer) patch.getOrDefault("actualMinutes", t.getActualMinutes()));
        if (patch.containsKey("recurrenceRule")) t.setRecurrenceRule(Objects.toString(patch.get("recurrenceRule"), t.getRecurrenceRule()));
        if (patch.containsKey("remindAt")) t.setRemindAt((LocalDateTime) patch.get("remindAt"));
    }

    private Map<String, Object> snapshot(Task t) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("title", t.getTitle());
        m.put("description", t.getDescription());
        m.put("status", t.getStatus());
        m.put("priority", t.getPriority());
        m.put("tags", t.getTags());
        m.put("startDate", t.getStartDate());
        m.put("dueDate", t.getDueDate());
        m.put("estimateMinutes", t.getEstimateMinutes());
        m.put("actualMinutes", t.getActualMinutes());
        m.put("recurrenceRule", t.getRecurrenceRule());
        m.put("remindAt", t.getRemindAt());
        return m;
    }

    private void recordDiffHistory(String taskId, String userId, Map<String, Object> before, Map<String, Object> after) {
        for (String k : before.keySet()) {
            Object a = before.get(k);
            Object b = after.get(k);
            if (!Objects.equals(a, b)) {
                recordHistory(taskId, userId, "UPDATE", k, String.valueOf(a), String.valueOf(b));
                if ("status".equals(k)) {
                    recordHistory(taskId, userId, "STATUS_CHANGE", k, String.valueOf(a), String.valueOf(b));
                }
            }
        }
    }

    private void recordHistory(String taskId, String userId, String action, String field, String oldVal, String newVal) {
        TaskHistory h = new TaskHistory();
        h.setTaskId(taskId);
        h.setActorUserId(userId);
        h.setAction(action);
        h.setField(field);
        h.setOldValue(oldVal);
        h.setNewValue(newVal);
        h.setCreatedAt(LocalDateTime.now());
        historyRepository.save(h);
    }

    private void generateNextOccurrence(Task current) {
        // 简化实现：只支持 FREQ=DAILY|WEEKLY|MONTHLY;INTERVAL=n
        String rule = current.getRecurrenceRule();
        if (!StringUtils.hasText(rule)) return;
        Map<String, String> kv = Arrays.stream(rule.split(";"))
                .map(s -> s.split("=")).filter(a -> a.length == 2)
                .collect(Collectors.toMap(a -> a[0].toUpperCase(), a -> a[1].toUpperCase()));
        String freq = kv.getOrDefault("FREQ", "DAILY");
        int interval = 1;
        try { interval = Integer.parseInt(kv.getOrDefault("INTERVAL", "1")); } catch (Exception ignore) {}

        Task next = new Task();
        next.setOwnerUserId(current.getOwnerUserId());
        next.setTitle(current.getTitle());
        next.setDescription(current.getDescription());
        next.setPriority(current.getPriority());
        next.setTags(current.getTags());
        next.setEstimateMinutes(current.getEstimateMinutes());
        next.setSeriesId(Optional.ofNullable(current.getSeriesId()).orElse(UUID.randomUUID().toString()));
        next.setRecurrenceRule(current.getRecurrenceRule());
        LocalDateTime start = Optional.ofNullable(current.getStartDate()).orElse(LocalDateTime.now());
        LocalDateTime due = Optional.ofNullable(current.getDueDate()).orElse(start);
        if ("WEEKLY".equals(freq)) {
            start = start.plusWeeks(interval);
            due = due.plusWeeks(interval);
        } else if ("MONTHLY".equals(freq)) {
            start = start.plusMonths(interval);
            due = due.plusMonths(interval);
        } else {
            start = start.plusDays(interval);
            due = due.plusDays(interval);
        }
        next.setStartDate(start);
        next.setDueDate(due);
        taskRepository.save(next);
    }

    private void assertCanRead(@NonNull String userId, Task task) {
        boolean owner = Objects.equals(task.getOwnerUserId(), userId);
        boolean shared = shareRepository.findByTaskId(task.getId()).stream()
                .anyMatch(s -> Objects.equals(s.getTargetUserId(), userId));
        if (!(owner || shared)) throw new RuntimeException("无权查看该任务");
    }

    private void assertCanEdit(@NonNull String userId, Task task) {
        boolean owner = Objects.equals(task.getOwnerUserId(), userId);
        boolean canEdit = shareRepository.existsByTaskIdAndTargetUserIdAndRole(
                Objects.requireNonNull(task.getId()),
                Objects.requireNonNull(userId),
                "EDIT");
        if (!(owner || canEdit)) throw new RuntimeException("无权编辑该任务");
    }
}


