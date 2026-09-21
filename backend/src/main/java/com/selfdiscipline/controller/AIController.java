package com.selfdiscipline.controller;

import com.selfdiscipline.dto.ChatRequest;
import com.selfdiscipline.dto.ChatResponse;
import com.selfdiscipline.dto.ConversationPatchRequest;
import com.selfdiscipline.model.Book;
import com.selfdiscipline.model.Chat;
import com.selfdiscipline.model.Conversation;
import com.selfdiscipline.model.Word;
import com.selfdiscipline.service.AIService;
import com.selfdiscipline.service.ImageService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/ai")
public class AIController {

    private final AIService aiService;
    private final ImageService imageService;

    public AIController(AIService aiService, ImageService imageService) {
        this.aiService = aiService;
        this.imageService = imageService;
    }

    @PostMapping("/conversations")
    public ResponseEntity<Conversation> createConversation(Authentication authentication) {
        return ResponseEntity.ok(aiService.createConversation(Objects.requireNonNull(authentication.getName())));
    }

    @GetMapping("/conversations")
    public ResponseEntity<List<Conversation>> listConversations(Authentication authentication) {
        return ResponseEntity.ok(aiService.listConversations(Objects.requireNonNull(authentication.getName())));
    }

    @PatchMapping("/conversations/{id}")
    public ResponseEntity<Conversation> renameConversation(@PathVariable String id,
                                                           @Valid @RequestBody ConversationPatchRequest request,
                                                           Authentication authentication) {
        return ResponseEntity.ok(aiService.renameConversation(
                Objects.requireNonNull(authentication.getName()), id, request.getTitle()));
    }

    @DeleteMapping("/conversations/{id}")
    public ResponseEntity<Void> deleteConversation(@PathVariable String id, Authentication authentication) {
        aiService.deleteConversation(Objects.requireNonNull(authentication.getName()), id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/conversations/{id}/messages")
    public ResponseEntity<List<Chat>> listMessages(@PathVariable String id, Authentication authentication) {
        return ResponseEntity.ok(aiService.listMessages(Objects.requireNonNull(authentication.getName()), id));
    }

    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(@Valid @RequestBody ChatRequest request, Authentication authentication) {
        return ResponseEntity.ok(aiService.chat(
                Objects.requireNonNull(authentication.getName()),
                request.getConversationId(),
                request.getQuestion(),
                request.getReplaceLast()));
    }

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_PLAIN_VALUE)
    public StreamingResponseBody chatStream(@Valid @RequestBody ChatRequest request, Authentication authentication) {
        ChatResponse response = aiService.chat(
                Objects.requireNonNull(authentication.getName()),
                request.getConversationId(),
                request.getQuestion(),
                request.getReplaceLast());
        String answer = response.getAnswer() == null ? "" : response.getAnswer();
        return outputStream -> {
            byte[] bytes = answer.getBytes(StandardCharsets.UTF_8);
            int step = 24;
            for (int i = 0; i < bytes.length; i += step) {
                int len = Math.min(step, bytes.length - i);
                outputStream.write(bytes, i, len);
                outputStream.flush();
            }
        };
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
