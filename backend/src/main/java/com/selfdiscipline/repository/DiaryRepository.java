package com.selfdiscipline.repository;

import com.selfdiscipline.model.Diary;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DiaryRepository extends MongoRepository<Diary, String> {
    List<Diary> findByUserIdOrderByDiaryDateDesc(String userId);
    Optional<Diary> findByUserIdAndDiaryDate(String userId, String diaryDate);
}
