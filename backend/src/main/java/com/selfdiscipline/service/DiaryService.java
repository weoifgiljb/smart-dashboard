package com.selfdiscipline.service;

import com.selfdiscipline.model.Diary;
import com.selfdiscipline.repository.DiaryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class DiaryService {

    @Autowired
    private DiaryRepository diaryRepository;

    public List<Diary> listDiaries(String userId) {
        return diaryRepository.findByUserIdOrderByDiaryDateDesc(userId);
    }

    public Diary createOrUpdateDiary(String userId, Diary diary) {
        diary.setUserId(userId);
        
        // Check if a diary already exists for this date
        Optional<Diary> existing = diaryRepository.findByUserIdAndDiaryDate(userId, diary.getDiaryDate());
        if (existing.isPresent()) {
            Diary dbDiary = existing.get();
            dbDiary.setContent(diary.getContent());
            dbDiary.setMood(diary.getMood());
            dbDiary.setTags(diary.getTags());
            dbDiary.setUpdatedAt(LocalDateTime.now());
            return diaryRepository.save(dbDiary);
        } else {
            diary.setCreatedAt(LocalDateTime.now());
            diary.setUpdatedAt(LocalDateTime.now());
            return diaryRepository.save(diary);
        }
    }

    public void deleteDiary(String userId, String id) {
        Optional<Diary> diary = diaryRepository.findById(id);
        if (diary.isPresent() && diary.get().getUserId().equals(userId)) {
            diaryRepository.deleteById(id);
        }
    }
    
    public Diary getDiary(String userId, String id) {
        return diaryRepository.findById(id)
                .filter(d -> d.getUserId().equals(userId))
                .orElse(null);
    }
}
