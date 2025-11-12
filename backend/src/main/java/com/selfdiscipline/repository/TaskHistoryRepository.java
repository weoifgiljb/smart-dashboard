package com.selfdiscipline.repository;

import com.selfdiscipline.model.TaskHistory;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskHistoryRepository extends MongoRepository<TaskHistory, String> {
    List<TaskHistory> findByTaskIdOrderByCreatedAtAsc(String taskId);
}



