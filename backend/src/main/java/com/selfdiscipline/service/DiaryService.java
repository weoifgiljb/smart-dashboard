package com.selfdiscipline.service;

import com.selfdiscipline.exception.ApiException;
import com.selfdiscipline.model.Diary;
import com.selfdiscipline.model.DiaryMood;
import com.selfdiscipline.model.User;
import com.selfdiscipline.repository.DiaryRepository;
import com.selfdiscipline.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;

@Service
public class DiaryService {

    @Autowired
    private DiaryRepository diaryRepository;

    @Autowired
    private UserRepository userRepository;

    public List<Diary> listDiaries(String username) {
        User user = requireUser(username);
        LinkedHashMap<String, Diary> byId = new LinkedHashMap<>();
        for (Diary diary : diaryRepository.findByUserIdOrderByDiaryDateDesc(user.getId())) {
            byId.put(diaryKey(diary), diary);
        }
        for (Diary diary : diaryRepository.findByUserIdOrderByDiaryDateDesc(username)) {
            byId.putIfAbsent(diaryKey(diary), diary);
        }
        return new ArrayList<>(byId.values());
    }

    public Diary createOrUpdateDiary(String username, Diary diary) {
        User user = requireUser(username);
        String ownerId = user.getId();
        diary.setUserId(ownerId);
        diary.setMood(DiaryMood.normalize(diary.getMood()));

        Optional<Diary> existing = diaryRepository.findByUserIdAndDiaryDate(ownerId, diary.getDiaryDate());
        if (existing.isEmpty()) {
            existing = diaryRepository.findByUserIdAndDiaryDate(username, diary.getDiaryDate());
        }
        if (existing.isPresent()) {
            Diary dbDiary = existing.get();
            dbDiary.setUserId(ownerId);
            dbDiary.setContent(diary.getContent());
            dbDiary.setMood(diary.getMood());
            dbDiary.setTags(diary.getTags());
            dbDiary.setImageUrl(diary.getImageUrl());
            dbDiary.setUpdatedAt(LocalDateTime.now());
            return diaryRepository.save(dbDiary);
        }
        diary.setCreatedAt(LocalDateTime.now());
        diary.setUpdatedAt(LocalDateTime.now());
        return diaryRepository.save(diary);
    }

    public void deleteDiary(String username, String id) {
        User user = requireUser(username);
        Diary diary = diaryRepository.findById(id)
                .filter(d -> owns(user, d))
                .orElseThrow(() -> ApiException.notFound("日记不存在"));
        diaryRepository.deleteById(diary.getId());
    }

    public Diary getDiary(String username, String id) {
        User user = requireUser(username);
        return diaryRepository.findById(id)
                .filter(d -> owns(user, d))
                .orElse(null);
    }

    private User requireUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> ApiException.notFound("用户不存在"));
    }

    private static boolean owns(User user, Diary diary) {
        String owner = diary.getUserId();
        return user.getId().equals(owner) || user.getUsername().equals(owner);
    }

    private static String diaryKey(Diary diary) {
        if (diary.getId() != null) {
            return diary.getId();
        }
        return String.valueOf(diary.getDiaryDate());
    }
}
