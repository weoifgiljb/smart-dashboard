package com.selfdiscipline.controller;

import com.selfdiscipline.dto.ChatRequest;
import com.selfdiscipline.dto.ChatResponse;
import com.selfdiscipline.model.Book;
import com.selfdiscipline.model.Chat;
import com.selfdiscipline.model.Word;
import com.selfdiscipline.service.AIService;
import com.selfdiscipline.service.ImageService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/ai")
public class AIController {

    private final AIService aiService;
    private final ImageService imageService;

    public AIController(AIService aiService, ImageService imageService) {
        this.aiService = aiService;
        this.imageService = imageService;
    }

    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(@Valid @RequestBody ChatRequest request, Authentication authentication) {
        return ResponseEntity.ok(aiService.chat(authentication.getName(), request.getQuestion()));
    }

    @GetMapping("/history")
    public ResponseEntity<List<Chat>> getHistory(Authentication authentication) {
        return ResponseEntity.ok(aiService.getHistory(authentication.getName()));
    }

    @PostMapping("/image/book/{bookId}")
    public ResponseEntity<Book> generateBookCover(@PathVariable String bookId,
                                                  @RequestParam(value = "force", required = false, defaultValue = "false") boolean force,
                                                  Authentication authentication) {
        return ResponseEntity.ok(imageService.generateBookCover(authentication.getName(), bookId, force));
    }

    @PostMapping("/image/word/{wordId}")
    public ResponseEntity<Word> generateWordImage(@PathVariable String wordId,
                                                  @RequestParam(value = "force", required = false, defaultValue = "false") boolean force,
                                                  Authentication authentication) {
        return ResponseEntity.ok(imageService.generateWordImage(authentication.getName(), wordId, force));
    }
}
