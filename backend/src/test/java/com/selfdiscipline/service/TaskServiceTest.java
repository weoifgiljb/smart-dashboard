package com.selfdiscipline.service;

import com.selfdiscipline.exception.ApiException;
import com.selfdiscipline.model.Task;
import com.selfdiscipline.model.TaskHistory;
import com.selfdiscipline.model.TaskShare;
import com.selfdiscipline.repository.TaskHistoryRepository;
import com.selfdiscipline.repository.TaskRepository;
import com.selfdiscipline.repository.TaskShareRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.selfdiscipline.testsupport.MockitoArgs.firstArg;
import static com.selfdiscipline.testsupport.MockitoArgs.nullableArg;
import static com.selfdiscipline.testsupport.MockitoArgs.stubSaveReturnsArg;
import static com.selfdiscipline.testsupport.MockitoArgs.verifyNeverSaved;
import static com.selfdiscipline.testsupport.MockitoArgs.verifySaved;
import static com.selfdiscipline.testsupport.MockitoArgs.whenSave;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;
    @Mock
    private TaskHistoryRepository historyRepository;
    @Mock
    private TaskShareRepository shareRepository;

    @InjectMocks
    private TaskService taskService;

    @Test
    void createTaskSetsOwnerAndRecordsHistory() {
        whenSave(taskRepository, Task.class).thenAnswer(inv -> {
            Task t = firstArg(inv, Task.class);
            t.setId("t1");
            return t;
        });
        stubSaveReturnsArg(historyRepository, TaskHistory.class);

        Task input = new Task();
        input.setTitle("写测试");
        Task saved = taskService.createTask("alice", input);

        assertEquals("alice", saved.getOwnerUserId());
        assertEquals("t1", saved.getId());
        TaskHistory history = verifySaved(historyRepository);
        assertEquals("CREATE", history.getAction());
        assertEquals("t1", history.getTaskId());
        assertEquals("alice", history.getActorUserId());
    }

    @Test
    void addActualMinutesAccumulatesWhenPositive() {
        Task existing = owned("t1", "alice");
        existing.setActualMinutes(10);
        when(taskRepository.findById("t1")).thenReturn(Optional.of(existing));
        when(shareRepository.findByTaskId("t1")).thenReturn(List.of());
        stubSaveReturnsArg(taskRepository, Task.class);
        stubSaveReturnsArg(historyRepository, TaskHistory.class);

        taskService.addActualMinutes("alice", "t1", 25);

        assertEquals(35, verifySaved(taskRepository).getActualMinutes());
    }

    @Test
    void addActualMinutesIgnoresNonPositive() {
        taskService.addActualMinutes("alice", "t1", 0);
        taskService.addActualMinutes("alice", "t1", -5);
        verify(taskRepository, never()).findById(nullableArg(String.class));
        verifyNeverSaved(taskRepository, Task.class);
    }

    @Test
    void listTasksMergesOwnedAndSharedThenFiltersStatus() {
        Task mine = owned("t1", "alice");
        mine.setStatus("todo");
        mine.setTitle("我的");
        Task shared = owned("t2", "bob");
        shared.setStatus("in_progress");
        shared.setTitle("分享来的");
        TaskShare share = new TaskShare();
        share.setTaskId("t2");
        share.setTargetUserId("alice");

        when(taskRepository.findByOwnerUserId("alice")).thenReturn(List.of(mine));
        when(shareRepository.findByTargetUserId("alice")).thenReturn(List.of(share));
        when(taskRepository.findAllById(Set.of("t2"))).thenReturn(List.of(shared));

        List<Task> all = taskService.listTasks("alice", Map.of());
        assertEquals(2, all.size());

        List<Task> todos = taskService.listTasks("alice", Map.of("status", "todo"));
        assertEquals(1, todos.size());
        assertEquals("t1", todos.get(0).getId());
    }

    @Test
    void getTaskRejectsUnrelatedUser() {
        Task task = owned("t1", "bob");
        when(taskRepository.findById("t1")).thenReturn(Optional.of(task));
        when(shareRepository.findByTaskId("t1")).thenReturn(List.of());

        ApiException ex = assertThrows(ApiException.class, () -> taskService.getTask("alice", "t1"));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    }

    @Test
    void getSubtasksQueriesByOwnerAndParent() {
        Task parent = owned("p1", "alice");
        Task child = owned("c1", "alice");
        child.setParentId("p1");
        when(taskRepository.findById("p1")).thenReturn(Optional.of(parent));
        when(shareRepository.findByTaskId("p1")).thenReturn(List.of());
        when(taskRepository.findByOwnerUserIdAndParentId("alice", "p1")).thenReturn(List.of(child));

        List<Task> subs = taskService.getSubtasks("alice", "p1");

        assertEquals(1, subs.size());
        assertEquals("c1", subs.get(0).getId());
        verify(taskRepository, never()).findAll();
        verify(taskRepository).findByOwnerUserIdAndParentId("alice", "p1");
    }

    @Test
    @SuppressWarnings("unchecked")
    void kanbanAndStatsTreatNullStatusAndPriorityAsDefaults() {
        Task orphan = owned("t1", "alice");
        orphan.setStatus(null);
        orphan.setPriority(null);
        when(taskRepository.findByOwnerUserId("alice")).thenReturn(List.of(orphan));
        when(shareRepository.findByTargetUserId("alice")).thenReturn(List.of());

        List<Map<String, Object>> board = taskService.getKanban("alice");
        assertEquals(1, board.size());
        assertEquals("todo", board.get(0).get("status"));
        assertEquals(1, ((List<Task>) board.get(0).get("items")).size());

        Map<String, Object> stats = taskService.getStats("alice");
        Map<String, Long> byStatus = (Map<String, Long>) stats.get("byStatus");
        Map<String, Long> byPriority = (Map<String, Long>) stats.get("byPriority");
        assertEquals(1L, byStatus.get("todo"));
        assertEquals(1L, byPriority.get("med"));
    }

    private static Task owned(String id, String owner) {
        Task task = new Task();
        task.setId(id);
        task.setOwnerUserId(owner);
        task.setTitle("task-" + id);
        return task;
    }
}
