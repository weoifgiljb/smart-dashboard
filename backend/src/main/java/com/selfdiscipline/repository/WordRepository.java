package com.selfdiscipline.repository;

import com.selfdiscipline.model.Word;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.time.LocalDateTime;

@Repository
public interface WordRepository extends MongoRepository<Word, String> {
    List<Word> findByUserIdOrderByCreateTimeDesc(String userId);
    long countByUserId(String userId);
    long countByUserIdAndStatus(String userId, String status);

    List<Word> findByUserIdAndStatusNotAndDueDateLessThanEqualOrderByDueDateAsc(String userId, String status, LocalDateTime dueDate);

    List<Word> findByUserIdAndBookOrderBySectionIndexAsc(String userId, String book);
}








