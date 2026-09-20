package com.selfdiscipline.controller;

import com.selfdiscipline.dto.DiaryRequest;
import com.selfdiscipline.dto.MemeMatchRequest;
import com.selfdiscipline.model.Diary;
import com.selfdiscipline.service.DiaryExportService;
import com.selfdiscipline.service.DiaryService;
import com.selfdiscipline.service.MemeService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/diaries")
public class DiaryController {

    private final DiaryService diaryService;
    private final DiaryExportService diaryExportService;
    private final MemeService memeService;

    public DiaryController(DiaryService diaryService, DiaryExportService diaryExportService, MemeService memeService) {
        this.diaryService = diaryService;
        this.diaryExportService = diaryExportService;
        this.memeService = memeService;
    }

    @GetMapping
    public ResponseEntity<List<Diary>> list(Authentication auth) {
        return ResponseEntity.ok(diaryService.listDiaries(Objects.requireNonNull(auth.getName())));
    }

    @PostMapping
    public ResponseEntity<Diary> createOrUpdate(@Valid @RequestBody DiaryRequest request, Authentication auth) {
        Diary diary = new Diary();
        diary.setContent(request.getContent());
        diary.setMood(request.getMood());
        diary.setTags(request.getTags());
        diary.setDiaryDate(request.getDiaryDate());
        diary.setImageUrl(request.getImageUrl());
        return ResponseEntity.ok(diaryService.createOrUpdateDiary(Objects.requireNonNull(auth.getName()), diary));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id, Authentication auth) {
        diaryService.deleteDiary(Objects.requireNonNull(auth.getName()), id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/export/pdf")
    public void exportPdf(Authentication auth, jakarta.servlet.http.HttpServletResponse response) throws IOException {
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=diaries.pdf");
        List<Diary> diaries = diaryService.listDiaries(Objects.requireNonNull(auth.getName()));
        diaryExportService.exportToPdf(diaries, response.getOutputStream());
    }

    @GetMapping("/export/word")
    public void exportWord(Authentication auth, jakarta.servlet.http.HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        response.setHeader("Content-Disposition", "attachment; filename=diaries.docx");
        List<Diary> diaries = diaryService.listDiaries(Objects.requireNonNull(auth.getName()));
        diaryExportService.exportToWord(diaries, response.getOutputStream());
    }

    @PostMapping("/match-meme")
    public ResponseEntity<String> matchMeme(@Valid @RequestBody MemeMatchRequest body) {
        return ResponseEntity.ok(memeService.findBestMeme(body.getContent()));
    }
}
