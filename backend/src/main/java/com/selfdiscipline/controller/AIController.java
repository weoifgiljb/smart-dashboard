package com.selfdiscipline.controller;

import com.selfdiscipline.dto.ChatRequest;
import com.selfdiscipline.model.Chat;
import com.selfdiscipline.service.AIService;
import com.selfdiscipline.service.ImageService;
import com.selfdiscipline.model.Book;
import com.selfdiscipline.model.Word;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/ai")
public class AIController {

    @Autowired
    private AIService aiService;

    @Autowired
    private ImageService imageService;

    @PostMapping("/chat")
    public ResponseEntity<Map<String, String>> chat(@RequestBody ChatRequest request, Authentication authentication) {
        Map<String, String> response = aiService.chat(authentication.getName(), request.getQuestion());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/history")
    public ResponseEntity<List<Chat>> getHistory(Authentication authentication) {
        List<Chat> history = aiService.getHistory(authentication.getName());
        return ResponseEntity.ok(history);
    }

    @PostMapping("/image/book/{bookId}")
    public ResponseEntity<Book> generateBookCover(@PathVariable String bookId,
                                                  @RequestParam(value = "force", required = false, defaultValue = "false") boolean force) {
        Book updated = imageService.generateBookCover(bookId, force);
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/image/word/{wordId}")
    public ResponseEntity<Word> generateWordImage(@PathVariable String wordId,
                                                  @RequestParam(value = "force", required = false, defaultValue = "false") boolean force,
                                                  Authentication authentication) {
        Word updated = imageService.generateWordImage(authentication.getName(), wordId, force);
        return ResponseEntity.ok(updated);
    }
}

