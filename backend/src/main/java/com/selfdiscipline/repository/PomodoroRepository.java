package com.selfdiscipline.repository;

import com.selfdiscipline.model.Pomodoro;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PomodoroRepository extends MongoRepository<Pomodoro, String> {
    List<Pomodoro> findByUserIdOrderByStartTimeDesc(String userId);
    long countByUserId(String userId);
    long countByUserIdAndStartTimeBetween(String userId, LocalDateTime start, LocalDateTime end);
}











