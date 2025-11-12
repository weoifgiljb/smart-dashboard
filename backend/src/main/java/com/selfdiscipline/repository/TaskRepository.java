package com.selfdiscipline.repository;

import com.selfdiscipline.model.Task;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TaskRepository extends MongoRepository<Task, String> {

    List<Task> findByOwnerUserId(String ownerUserId);

    List<Task> findByOwnerUserIdAndStatusIn(String ownerUserId, List<String> statuses);

    List<Task> findByOwnerUserIdAndDueDateBetween(String ownerUserId, LocalDateTime start, LocalDateTime end);
}



