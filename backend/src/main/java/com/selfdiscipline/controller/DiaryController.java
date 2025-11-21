package com.selfdiscipline.controller;

import com.selfdiscipline.model.Diary;
import com.selfdiscipline.service.DiaryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/diaries")
public class DiaryController {

    @Autowired
    private DiaryService diaryService;

    @GetMapping
    public ResponseEntity<List<Diary>> list(Authentication auth) {
        return ResponseEntity.ok(diaryService.listDiaries(Objects.requireNonNull(auth.getName())));
    }

    @PostMapping
    public ResponseEntity<Diary> createOrUpdate(@RequestBody Diary diary, Authentication auth) {
        return ResponseEntity.ok(diaryService.createOrUpdateDiary(Objects.requireNonNull(auth.getName()), diary));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id, Authentication auth) {
        diaryService.deleteDiary(Objects.requireNonNull(auth.getName()), id);
        return ResponseEntity.ok().build();
    }
}
