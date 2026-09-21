package com.selfdiscipline.service;

import com.selfdiscipline.config.AliyunAIConfig;
import com.selfdiscipline.config.OllamaConfig;
import com.selfdiscipline.dto.ChatResponse;
import com.selfdiscipline.dto.RhythmResponse;
import com.selfdiscipline.exception.ApiException;
import com.selfdiscipline.model.Chat;
import com.selfdiscipline.model.Conversation;
import com.selfdiscipline.model.User;
import com.selfdiscipline.repository.ChatRepository;
import com.selfdiscipline.repository.ConversationRepository;
import com.selfdiscipline.repository.UserRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AIService {

    private static final Logger log = LoggerFactory.getLogger(AIService.class);
    private static final String FALLBACK_ANSWER = "抱歉，AI服务暂时不可用，请稍后重试。";
    public static final String DEFAULT_TITLE = "新对话";
    public static final String LEGACY_TITLE = "历史对话";
    public static final int AUTO_TITLE_CHARS = 24;
    public static final int CONTEXT_TURNS = 10;

    private final ChatRepository chatRepository;
    private final ConversationRepository conversationRepository;
    private final UserRepository userRepository;
    private final AliyunAIConfig aliyunAIConfig;
    private final OllamaConfig ollamaConfig;
    private final WebClient webClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private DashboardService dashboardService;

    @Autowired
    public AIService(ChatRepository chatRepository,
                     ConversationRepository conversationRepository,
                     UserRepository userRepository,
                     AliyunAIConfig aliyunAIConfig,
                     OllamaConfig ollamaConfig) {
        this(chatRepository, conversationRepository, userRepository, aliyunAIConfig, ollamaConfig, WebClient.create());
    }

    AIService(ChatRepository chatRepository,
              ConversationRepository conversationRepository,
              UserRepository userRepository,
              AliyunAIConfig aliyunAIConfig,
              OllamaConfig ollamaConfig,
              WebClient webClient) {
        this.chatRepository = chatRepository;
        this.conversationRepository = conversationRepository;
        this.userRepository = userRepository;
        this.aliyunAIConfig = aliyunAIConfig;
        this.ollamaConfig = ollamaConfig;
        this.webClient = webClient;
    }

    @Autowired(required = false)
    void setDashboardService(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    String rhythmContext(String username) {
        if (dashboardService == null) {
            return "";
        }
        try {
            RhythmResponse rhythm = dashboardService.getRhythm(username);
            String task = rhythm.getFocusTask() == null ? "无" : rhythm.getFocusTask().getTitle();
            int heat = rhythm.getHeat() == null ? 0 : rhythm.getHeat().getTotal();
            return "今日节律（只读，不要改数据）：下一动作=" + rhythm.getNextAction()
                    + "，到期词=" + rhythm.getDueWordCount()
                    + "，专注任务=" + task
                    + "，今日热力=" + heat
                    + "。请据此帮助用户安排今晚或解释热力，不要声称已改任务或打卡。";
        } catch (Exception e) {
            log.debug("注入节律上下文失败: {}", e.getMessage());
            return "";
        }
    }

    public Conversation createConversation(String username) {
        User user = requireUser(username);
        Conversation empty = findEmptyConversation(user.getId());
        if (empty != null) {
            return empty;
        }
        Conversation created = new Conversation();
        created.setUserId(user.getId());
        created.setTitle(DEFAULT_TITLE);
        LocalDateTime now = LocalDateTime.now();
        created.setCreatedAt(now);
        created.setUpdatedAt(now);
        return conversationRepository.save(created);
    }

    public List<Conversation> listConversations(String username) {
        User user = requireUser(username);
        migrateLegacyChats(user);
        return conversationRepository.findByUserIdOrderByUpdatedAtDesc(user.getId());
    }

    public Conversation renameConversation(String username, String conversationId, String title) {
        Conversation conversation = requireOwnedConversation(username, conversationId);
        String trimmed = title == null ? "" : title.trim();
        if (trimmed.isEmpty()) {
            throw ApiException.badRequest("标题不能为空");
        }
        if (trimmed.length() > 40) {
            throw ApiException.badRequest("标题最多 40 字");
        }
        conversation.setTitle(trimmed);
        conversation.setUpdatedAt(LocalDateTime.now());
        return conversationRepository.save(conversation);
    }

    public void deleteConversation(String username, String conversationId) {
        Conversation conversation = requireOwnedConversation(username, conversationId);
        chatRepository.deleteByConversationIdAndUserId(conversationId, conversation.getUserId());
        conversationRepository.delete(conversation);
    }

    public List<Chat> listMessages(String username, String conversationId) {
        Conversation conversation = requireOwnedConversation(username, conversationId);
        return chatRepository.findByConversationIdAndUserIdOrderByCreateTimeAsc(
                conversation.getId(), conversation.getUserId());
    }

    public ChatResponse chat(String username, String conversationId, String question) {
        return chat(username, conversationId, question, false);
    }

    public ChatResponse chat(String username, String conversationId, String question, boolean replaceLast) {
        Conversation conversation = requireOwnedConversation(username, conversationId);
        List<Chat> history = chatRepository.findByConversationIdAndUserIdOrderByCreateTimeAsc(
                conversation.getId(), conversation.getUserId());
        Chat lastTurn = null;
        List<Chat> contextHistory = history;
        if (replaceLast) {
            if (history.isEmpty()) {
                throw ApiException.badRequest("没有可重新生成的消息");
            }
            lastTurn = history.get(history.size() - 1);
            if (!nullToEmpty(question).equals(nullToEmpty(lastTurn.getQuestion()))) {
                throw ApiException.badRequest("只能重新生成最后一轮对话");
            }
            contextHistory = history.subList(0, history.size() - 1);
        }
        String answer = complete(buildLlmMessages(contextHistory, question, rhythmContext(username)));
        if (lastTurn != null) {
            lastTurn.setAnswer(answer);
            chatRepository.save(lastTurn);
            conversation.setUpdatedAt(LocalDateTime.now());
            conversationRepository.save(conversation);
        } else {
            persistChat(conversation.getUserId(), conversation.getId(), question, answer);
            touchConversation(conversation, question);
        }
        return new ChatResponse(answer);
    }

    static List<Map<String, String>> buildLlmMessages(List<Chat> history, String question) {
        return buildLlmMessages(history, question, "");
    }

    static List<Map<String, String>> buildLlmMessages(List<Chat> history, String question, String rhythmContext) {
        List<Chat> turns = history == null ? List.of() : history;
        int start = Math.max(0, turns.size() - CONTEXT_TURNS);
        List<Map<String, String>> messages = new ArrayList<>();
        if (rhythmContext != null && !rhythmContext.isBlank()) {
            messages.add(Map.of("role", "system", "content", rhythmContext));
        }
        for (int i = start; i < turns.size(); i++) {
            Chat turn = turns.get(i);
            messages.add(Map.of("role", "user", "content", nullToEmpty(turn.getQuestion())));
            messages.add(Map.of("role", "assistant", "content", nullToEmpty(turn.getAnswer())));
        }
        messages.add(Map.of("role", "user", "content", nullToEmpty(question)));
        return messages;
    }

    String complete(List<Map<String, String>> messages) {
        String answer = tryOllama(messages);
        if (answer == null || answer.isBlank()) {
            answer = tryAliyun(messages);
        }
        if (answer == null || answer.isBlank()) {
            answer = FALLBACK_ANSWER;
        }
        return answer;
    }

    private String tryOllama(List<Map<String, String>> messages) {
        String chatAnswer = tryOllamaChat(messages);
        if (chatAnswer != null && !chatAnswer.isBlank()) {
            return chatAnswer;
        }
        return tryOllamaGenerate(messages);
    }

    private String tryOllamaChat(List<Map<String, String>> messages) {
        try {
            Map<String, Object> ollamaBody = new HashMap<>();
            ollamaBody.put("model", ollamaConfig.getModel());
            ollamaBody.put("messages", messages);
            ollamaBody.put("stream", false);

            String ollamaResponse = webClient.post()
                    .uri(ollamaConfig.getBaseUrl() + "/api/chat")
                    .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .bodyValue(ollamaBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode ollamaNode = objectMapper.readTree(ollamaResponse);
            String ollamaAnswer = ollamaNode.path("message").path("content").asText(null);
            if (ollamaAnswer != null && !ollamaAnswer.isBlank()) {
                return ollamaAnswer;
            }
        } catch (Exception e) {
            log.debug("Ollama chat 调用失败，将回退 generate: {}", e.getMessage());
        }
        return null;
    }

    private String tryOllamaGenerate(List<Map<String, String>> messages) {
        try {
            Map<String, Object> ollamaBody = new HashMap<>();
            ollamaBody.put("model", ollamaConfig.getModel());
            ollamaBody.put("prompt", concatPrompt(messages));
            ollamaBody.put("stream", false);

            String ollamaResponse = webClient.post()
                    .uri(ollamaConfig.getBaseUrl() + "/api/generate")
                    .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .bodyValue(ollamaBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode ollamaNode = objectMapper.readTree(ollamaResponse);
            String ollamaAnswer = ollamaNode.path("response").asText(null);
            if (ollamaAnswer != null && !ollamaAnswer.isBlank()) {
                return ollamaAnswer;
            }
        } catch (Exception e) {
            log.debug("Ollama 调用失败，将回退: {}", e.getMessage());
        }
        return null;
    }

    private String tryAliyun(List<Map<String, String>> messages) {
        try {
            String apiKey = aliyunAIConfig.getApiKey();
            if (apiKey == null || apiKey.isBlank() || "YOUR_API_KEY".equals(apiKey)) {
                return null;
            }
            Map<String, Object> requestBody = new HashMap<>();
            Map<String, Object> input = new HashMap<>();
            input.put("messages", messages);
            String model = aliyunAIConfig.getModel() == null || aliyunAIConfig.getModel().isBlank()
                    ? "qwen-turbo" : aliyunAIConfig.getModel();
            requestBody.put("model", model);
            requestBody.put("input", input);

            String apiUrl = aliyunAIConfig.getApiUrl();
            String response = webClient.post()
                    .uri(apiUrl != null ? apiUrl : "")
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode jsonNode = objectMapper.readTree(response);
            String aliyunAnswer = jsonNode.path("output").path("choices").get(0).path("message").path("content").asText(null);
            if (aliyunAnswer != null && !aliyunAnswer.isBlank()) {
                return aliyunAnswer;
            }
        } catch (Exception e) {
            log.debug("通义千问调用失败: {}", e.getMessage());
        }
        return null;
    }

    void persistChat(String userId, String conversationId, String question, String answer) {
        Chat chat = new Chat();
        chat.setUserId(userId);
        chat.setConversationId(conversationId);
        chat.setQuestion(question);
        chat.setAnswer(answer);
        chat.setCreateTime(LocalDateTime.now());
        chatRepository.save(chat);
    }

    public List<Double> generateEmbeddings(String text) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("model", ollamaConfig.getModel());
            body.put("prompt", text);

            String response = webClient.post()
                    .uri(ollamaConfig.getBaseUrl() + "/api/embeddings")
                    .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode node = objectMapper.readTree(response);
            JsonNode embeddingNode = node.path("embedding");
            if (embeddingNode.isArray()) {
                return objectMapper.convertValue(embeddingNode, new TypeReference<List<Double>>() {});
            }
        } catch (Exception e) {
            log.warn("生成 embedding 失败: {}", e.getMessage());
        }
        return List.of();
    }

    public List<Chat> getHistory(String username) {
        User user = requireUser(username);
        return chatRepository.findByUserIdOrderByCreateTimeDesc(user.getId());
    }

    private void migrateLegacyChats(User user) {
        List<Chat> orphans = chatRepository.findByUserIdAndConversationIdIsNull(user.getId());
        if (orphans.isEmpty()) {
            return;
        }
        LocalDateTime latest = orphans.stream()
                .map(Chat::getCreateTime)
                .filter(time -> time != null)
                .max(LocalDateTime::compareTo)
                .orElse(LocalDateTime.now());
        LocalDateTime earliest = orphans.stream()
                .map(Chat::getCreateTime)
                .filter(time -> time != null)
                .min(LocalDateTime::compareTo)
                .orElse(latest);
        List<Conversation> existingLegacy = conversationRepository.findByUserIdAndTitle(user.getId(), LEGACY_TITLE);
        Conversation legacy;
        if (!existingLegacy.isEmpty()) {
            legacy = existingLegacy.get(0);
            if (legacy.getUpdatedAt() == null || latest.isAfter(legacy.getUpdatedAt())) {
                legacy.setUpdatedAt(latest);
                conversationRepository.save(legacy);
            }
        } else {
            legacy = new Conversation();
            legacy.setUserId(user.getId());
            legacy.setTitle(LEGACY_TITLE);
            legacy.setCreatedAt(earliest);
            legacy.setUpdatedAt(latest);
            legacy = conversationRepository.save(legacy);
        }
        for (Chat chat : orphans) {
            chat.setConversationId(legacy.getId());
            chatRepository.save(chat);
        }
    }

    private Conversation findEmptyConversation(String userId) {
        List<Conversation> conversations = conversationRepository.findByUserIdOrderByUpdatedAtDesc(userId);
        for (Conversation conversation : conversations) {
            if (!chatRepository.existsByConversationId(conversation.getId())) {
                return conversation;
            }
        }
        return null;
    }

    private void touchConversation(Conversation conversation, String question) {
        if (DEFAULT_TITLE.equals(conversation.getTitle())) {
            conversation.setTitle(autoTitle(question));
        }
        conversation.setUpdatedAt(LocalDateTime.now());
        conversationRepository.save(conversation);
    }

    static String autoTitle(String question) {
        String trimmed = question == null ? "" : question.trim();
        if (trimmed.length() <= AUTO_TITLE_CHARS) {
            return trimmed.isEmpty() ? DEFAULT_TITLE : trimmed;
        }
        return trimmed.substring(0, AUTO_TITLE_CHARS);
    }

    private Conversation requireOwnedConversation(String username, String conversationId) {
        User user = requireUser(username);
        if (conversationId == null || conversationId.isBlank()) {
            throw ApiException.badRequest("会话不能为空");
        }
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> ApiException.notFound("会话不存在"));
        if (!user.getId().equals(conversation.getUserId())) {
            throw ApiException.forbidden("无权访问该会话");
        }
        return conversation;
    }

    private User requireUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> ApiException.notFound("用户不存在"));
    }

    private static String concatPrompt(List<Map<String, String>> messages) {
        StringBuilder prompt = new StringBuilder();
        for (Map<String, String> message : messages) {
            String role = message.getOrDefault("role", "user");
            String content = message.getOrDefault("content", "");
            if ("assistant".equals(role)) {
                prompt.append("Assistant: ").append(content).append('\n');
            } else {
                prompt.append("User: ").append(content).append('\n');
            }
        }
        return prompt.toString();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
