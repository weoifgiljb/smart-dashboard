package com.selfdiscipline.controller;

import com.selfdiscipline.dto.ChatRequest;
import com.selfdiscipline.model.Chat;
import com.selfdiscipline.model.Conversation;
import com.selfdiscipline.model.Book;
import com.selfdiscipline.model.Word;
import com.selfdiscipline.service.AIService;
import com.selfdiscipline.service.ImageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

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
        Map<String, String> response = aiService.chat(authentication.getName(), request.getQuestion(), request.getConversationId());
        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamChat(@RequestBody ChatRequest request, Authentication authentication) {
        return aiService.streamChat(authentication.getName(), request.getQuestion(), request.getConversationId());
    }

    @PostMapping("/conversations")
    public ResponseEntity<Conversation> createConversation(Authentication authentication, @RequestBody(required = false) Map<String, String> body) {
        try {
            String title = body != null ? body.get("title") : null;
            Conversation conversation = aiService.createConversation(authentication.getName(), title);
            return ResponseEntity.ok(conversation);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    @GetMapping("/conversations")
    public ResponseEntity<List<Conversation>> getConversations(Authentication authentication) {
        try {
            List<Conversation> conversations = aiService.getConversations(authentication.getName());
            return ResponseEntity.ok(conversations);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    @GetMapping("/history")
    public ResponseEntity<List<Chat>> getHistory(Authentication authentication, @RequestParam(required = false) String conversationId) {
        List<Chat> history = aiService.getHistory(authentication.getName(), conversationId);
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
