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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
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
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> {
            Task t = inv.getArgument(0);
            t.setId("t1");
            return t;
        });
        when(historyRepository.save(any(TaskHistory.class))).thenAnswer(inv -> inv.getArgument(0));

        Task input = new Task();
        input.setTitle("写测试");
        Task saved = taskService.createTask("alice", input);

        assertEquals("alice", saved.getOwnerUserId());
        assertEquals("t1", saved.getId());
        ArgumentCaptor<TaskHistory> captor = ArgumentCaptor.forClass(TaskHistory.class);
        verify(historyRepository).save(captor.capture());
        assertEquals("CREATE", captor.getValue().getAction());
        assertEquals("t1", captor.getValue().getTaskId());
        assertEquals("alice", captor.getValue().getActorUserId());
    }

    @Test
    void addActualMinutesAccumulatesWhenPositive() {
        Task existing = owned("t1", "alice");
        existing.setActualMinutes(10);
        when(taskRepository.findById("t1")).thenReturn(Optional.of(existing));
        when(shareRepository.findByTaskId("t1")).thenReturn(List.of());
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));
        when(historyRepository.save(any(TaskHistory.class))).thenAnswer(inv -> inv.getArgument(0));

        taskService.addActualMinutes("alice", "t1", 25);

        ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
        verify(taskRepository).save(captor.capture());
        assertEquals(35, captor.getValue().getActualMinutes());
    }

    @Test
    void addActualMinutesIgnoresNonPositive() {
        taskService.addActualMinutes("alice", "t1", 0);
        taskService.addActualMinutes("alice", "t1", -5);
        verify(taskRepository, never()).findById(any());
        verify(taskRepository, never()).save(any());
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

    private static Task owned(String id, String owner) {
        Task task = new Task();
        task.setId(id);
        task.setOwnerUserId(owner);
        task.setTitle("task-" + id);
        return task;
    }
}
