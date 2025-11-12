package com.selfdiscipline.repository;

import com.selfdiscipline.model.TaskShare;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskShareRepository extends MongoRepository<TaskShare, String> {

    boolean existsByTaskIdAndTargetUserIdAndRole(String taskId, String targetUserId, String role);

    List<TaskShare> findByTaskId(String taskId);

    List<TaskShare> findByTargetUserId(String targetUserId);
}



