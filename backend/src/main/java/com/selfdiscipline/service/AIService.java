package com.selfdiscipline.service;

import com.selfdiscipline.config.AliyunAIConfig;
import com.selfdiscipline.config.OllamaConfig;
import com.selfdiscipline.dto.ChatResponse;
import com.selfdiscipline.exception.ApiException;
import com.selfdiscipline.model.Chat;
import com.selfdiscipline.model.User;
import com.selfdiscipline.repository.ChatRepository;
import com.selfdiscipline.repository.UserRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AIService {

    private static final Logger log = LoggerFactory.getLogger(AIService.class);
    private static final String FALLBACK_ANSWER = "抱歉，AI服务暂时不可用，请稍后重试。";

    private final ChatRepository chatRepository;
    private final UserRepository userRepository;
    private final AliyunAIConfig aliyunAIConfig;
    private final OllamaConfig ollamaConfig;
    private final WebClient webClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AIService(ChatRepository chatRepository,
                     UserRepository userRepository,
                     AliyunAIConfig aliyunAIConfig,
                     OllamaConfig ollamaConfig) {
        this(chatRepository, userRepository, aliyunAIConfig, ollamaConfig, WebClient.create());
    }

    AIService(ChatRepository chatRepository,
              UserRepository userRepository,
              AliyunAIConfig aliyunAIConfig,
              OllamaConfig ollamaConfig,
              WebClient webClient) {
        this.chatRepository = chatRepository;
        this.userRepository = userRepository;
        this.aliyunAIConfig = aliyunAIConfig;
        this.ollamaConfig = ollamaConfig;
        this.webClient = webClient;
    }

    public ChatResponse chat(String username, String question) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> ApiException.notFound("用户不存在"));

        String answer = complete(question);
        persistChat(user.getId(), question, answer);

        return new ChatResponse(answer);
    }

    String complete(String question) {
        String answer = tryOllama(question);
        if (answer == null || answer.isBlank()) {
            answer = tryAliyun(question);
        }
        if (answer == null || answer.isBlank()) {
            answer = FALLBACK_ANSWER;
        }
        return answer;
    }

    private String tryOllama(String question) {
        try {
            Map<String, Object> ollamaBody = new HashMap<>();
            ollamaBody.put("model", ollamaConfig.getModel());
            ollamaBody.put("prompt", question);
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

    private String tryAliyun(String question) {
        try {
            String apiKey = aliyunAIConfig.getApiKey();
            if (apiKey == null || apiKey.isBlank() || "YOUR_API_KEY".equals(apiKey)) {
                return null;
            }
            Map<String, Object> requestBody = new HashMap<>();
            Map<String, Object> input = new HashMap<>();
            input.put("messages", List.of(Map.of("role", "user", "content", question)));
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

    void persistChat(String userId, String question, String answer) {
        Chat chat = new Chat();
        chat.setUserId(userId);
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
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> ApiException.notFound("用户不存在"));
        return chatRepository.findByUserIdOrderByCreateTimeDesc(user.getId());
    }
}
