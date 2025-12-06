package com.selfdiscipline.service;

import com.selfdiscipline.config.AliyunAIConfig;
import com.selfdiscipline.config.OllamaConfig;
import com.selfdiscipline.model.Chat;
import com.selfdiscipline.model.Conversation;
import com.selfdiscipline.model.User;
import com.selfdiscipline.repository.ChatRepository;
import com.selfdiscipline.repository.ConversationRepository;
import com.selfdiscipline.repository.UserRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class AIService {

	@Autowired
	private ChatRepository chatRepository;

	@Autowired
	private ConversationRepository conversationRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private AliyunAIConfig aliyunAIConfig;

	@Autowired
	private OllamaConfig ollamaConfig;

	private final WebClient webClient = WebClient.create();
	private final ObjectMapper objectMapper = new ObjectMapper();

	public Conversation createConversation(String username, String title) {
		User user = userRepository.findByUsername(username)
				.orElseThrow(() -> new RuntimeException("用户不存在"));
		Conversation conversation = new Conversation();
		conversation.setUserId(user.getId());
		conversation.setTitle(title != null && !title.isEmpty() ? title : "新对话");
		return conversationRepository.save(conversation);
	}

	public List<Conversation> getConversations(String username) {
		User user = userRepository.findByUsername(username)
				.orElseThrow(() -> new RuntimeException("用户不存在"));
		return conversationRepository.findByUserIdOrderByUpdateTimeDesc(user.getId());
	}

	public Map<String, String> chat(String username, String question, String conversationId) {
		User user = userRepository.findByUsername(username)
				.orElseThrow(() -> new RuntimeException("用户不存在"));

		// Generate answer
		String answer = generateAnswer(question);

		// Save chat
		saveChat(user, question, answer, conversationId);

		Map<String, String> result = new HashMap<>();
		result.put("answer", answer);
		return result;
	}

    public Flux<String> streamChat(String username, String question, String conversationId) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        Map<String, Object> ollamaBody = new HashMap<>();
        ollamaBody.put("model", ollamaConfig.getModel());
        ollamaBody.put("prompt", question);
        ollamaBody.put("stream", true);

        StringBuilder fullResponse = new StringBuilder();

        return webClient.post()
                .uri(ollamaConfig.getBaseUrl() + "/api/generate")
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .bodyValue(ollamaBody)
                .retrieve()
                .bodyToFlux(String.class)
                .map(json -> {
                    try {
                        JsonNode node = objectMapper.readTree(json);
                        String chunk = node.path("response").asText("");
                        fullResponse.append(chunk);
                        return chunk;
                    } catch (Exception e) {
                        return "";
                    }
                })
                .doOnComplete(() -> {
                    String answer = fullResponse.toString();
                    if (!answer.isBlank()) {
                        saveChat(user, question, answer, conversationId);
                    }
                })
                .onErrorResume(e -> {
                    System.err.println("Stream chat error: " + e.getMessage());
                    return Flux.just("Error: " + e.getMessage());
                });
    }

	private void saveChat(User user, String question, String answer, String conversationId) {
		if (conversationId != null && !conversationId.isEmpty()) {
			Optional<Conversation> convOpt = conversationRepository.findById(conversationId);
			if (convOpt.isPresent()) {
				Conversation conv = convOpt.get();
				if ("新对话".equals(conv.getTitle())) {
					String newTitle = question.length() > 20 ? question.substring(0, 20) + "..." : question;
					conv.setTitle(newTitle);
				}
				conv.setUpdateTime(LocalDateTime.now());
				conversationRepository.save(conv);

				Chat chat = new Chat();
				chat.setUserId(user.getId());
				chat.setConversationId(conversationId);
				chat.setQuestion(question);
				chat.setAnswer(answer);
				chatRepository.save(chat);
			}
		} else {
            // Fallback for no conversation ID (backward compatibility)
            Chat chat = new Chat();
            chat.setUserId(user.getId());
            chat.setQuestion(question);
            chat.setAnswer(answer);
            chatRepository.save(chat);
        }
	}

	// Keep old signature for compatibility
	public Map<String, String> chat(String username, String question) {
		return chat(username, question, null);
	}

	private String generateAnswer(String question) {
		String answer = null;

		// 优先尝试调用本地 Ollama
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
				answer = ollamaAnswer;
			}
		} catch (Exception ignored) {
		}

		// 回退到阿里通义千问
		if (answer == null || answer.isBlank()) {
			try {
				Map<String, Object> requestBody = new HashMap<>();
				Map<String, Object> input = new HashMap<>();
				input.put("messages", List.of(Map.of("role", "user", "content", question)));
				requestBody.put("model", "qwen-turbo");
				requestBody.put("input", input);

				String apiUrl = aliyunAIConfig.getApiUrl();
				String apiKey = aliyunAIConfig.getApiKey();
				String response = webClient.post()
						.uri(apiUrl != null ? apiUrl : "")
						.header("Authorization", "Bearer " + (apiKey != null ? apiKey : ""))
						.header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
						.bodyValue(requestBody)
						.retrieve()
						.bodyToMono(String.class)
						.block();

				JsonNode jsonNode = objectMapper.readTree(response);
				String aliyunAnswer = jsonNode.path("output").path("choices").get(0).path("message").path("content").asText(null);
				if (aliyunAnswer != null && !aliyunAnswer.isBlank()) {
					answer = aliyunAnswer;
				}
			} catch (Exception ignored) {
			}
		}

		// 最终回退
		if (answer == null || answer.isBlank()) {
			answer = "抱歉，AI服务暂时不可用，请稍后重试。";
		}
		return answer;
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
			e.printStackTrace();
		}
		return List.of();
	}

	public List<Chat> getHistory(String username, String conversationId) {
		if (conversationId != null && !conversationId.isEmpty()) {
			return chatRepository.findByConversationIdOrderByCreateTimeAsc(conversationId);
		}
		User user = userRepository.findByUsername(username)
				.orElseThrow(() -> new RuntimeException("用户不存在"));
		return chatRepository.findByUserIdOrderByCreateTimeDesc(user.getId());
	}
    
    // Compatibility wrapper
    public List<Chat> getHistory(String username) {
        return getHistory(username, null);
    }
}
