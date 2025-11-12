package com.selfdiscipline.controller;

import com.selfdiscipline.dto.WordRequest;
import com.selfdiscipline.dto.WordImportRequest;
import com.selfdiscipline.dto.WordStatusRequest;
import com.selfdiscipline.model.Word;
import com.selfdiscipline.service.WordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/words")
public class WordController {

    @Autowired
    private WordService wordService;

    @GetMapping
    public ResponseEntity<List<Word>> getWords(Authentication authentication) {
        List<Word> words = wordService.getWords(authentication.getName());
        return ResponseEntity.ok(words);
    }

    @GetMapping("/today")
    public ResponseEntity<List<Word>> getTodayWords(Authentication authentication) {
        List<Word> words = wordService.getTodayWords(authentication.getName());
        return ResponseEntity.ok(words);
    }

    @GetMapping("/books")
    public ResponseEntity<List<Map<String, Object>>> getBooks(Authentication authentication) {
        List<Map<String, Object>> books = wordService.getBooks(authentication.getName());
        return ResponseEntity.ok(books);
    }

    @PostMapping
    public ResponseEntity<Word> addWord(@RequestBody WordRequest request, Authentication authentication) {
        Word word = wordService.addWord(authentication.getName(), request);
        return ResponseEntity.ok(word);
    }

    @PostMapping("/import")
    public ResponseEntity<Map<String, Object>> importWords(@RequestBody WordImportRequest req, Authentication authentication) {
        Map<String, Object> result = wordService.importWords(authentication.getName(), req);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/import-default")
    public ResponseEntity<Map<String, Object>> importDefault(Authentication authentication) {
        Map<String, Object> result = wordService.importDefaultWords(authentication.getName());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{id}/status")
    public ResponseEntity<Word> updateStatus(@PathVariable @NonNull String id, @RequestBody WordStatusRequest req, Authentication authentication) {
        Word word = wordService.updateWordStatus(authentication.getName(), id, req);
        return ResponseEntity.ok(word);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteWord(@PathVariable @NonNull String id, Authentication authentication) {
        wordService.deleteWord(authentication.getName(), id);
        return ResponseEntity.ok(Map.of("message", "删除成功"));
    }

    @PostMapping("/{id}/review")
    public ResponseEntity<Word> reviewWord(@PathVariable @NonNull String id, Authentication authentication) {
        Word word = wordService.reviewWord(authentication.getName(), id);
        return ResponseEntity.ok(word);
    }
}

