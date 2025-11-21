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

    @Autowired
    private com.selfdiscipline.service.DiaryExportService diaryExportService;

    @Autowired
    private com.selfdiscipline.service.MemeService memeService;

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

    @GetMapping("/export/pdf")
    public void exportPdf(Authentication auth, jakarta.servlet.http.HttpServletResponse response) throws java.io.IOException {
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=diaries.pdf");
        List<Diary> diaries = diaryService.listDiaries(Objects.requireNonNull(auth.getName()));
        diaryExportService.exportToPdf(diaries, response.getOutputStream());
    }

    @GetMapping("/export/word")
    public void exportWord(Authentication auth, jakarta.servlet.http.HttpServletResponse response) throws java.io.IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        response.setHeader("Content-Disposition", "attachment; filename=diaries.docx");
        List<Diary> diaries = diaryService.listDiaries(Objects.requireNonNull(auth.getName()));
        diaryExportService.exportToWord(diaries, response.getOutputStream());
    }

    @PostMapping("/match-meme")
    public ResponseEntity<String> matchMeme(@RequestBody java.util.Map<String, String> body) {
        String text = body.get("content");
        String url = memeService.findBestMeme(text);
        return ResponseEntity.ok(url);
    }
}
