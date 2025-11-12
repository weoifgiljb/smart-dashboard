package com.selfdiscipline.service;

import com.selfdiscipline.config.AliyunAIConfig;
import com.selfdiscipline.config.OllamaConfig;
import com.selfdiscipline.model.Chat;
import com.selfdiscipline.model.User;
import com.selfdiscipline.repository.ChatRepository;
import com.selfdiscipline.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AIService {

	@Autowired
	private ChatRepository chatRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private AliyunAIConfig aliyunAIConfig;

	@Autowired
	private OllamaConfig ollamaConfig;

	private final WebClient webClient = WebClient.create();
	private final ObjectMapper objectMapper = new ObjectMapper();

	public Map<String, String> chat(String username, String question) {
		User user = userRepository.findByUsername(username)
				.orElseThrow(() -> new RuntimeException("用户不存在"));

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
			// 忽略，进入下一步回退
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
				// 忽略，进入最终回退
			}
		}

		// 最终回退
		if (answer == null || answer.isBlank()) {
			answer = "抱歉，AI服务暂时不可用，请稍后重试。";
		}

		// 保存对话记录
		Chat chat = new Chat();
		chat.setUserId(user.getId());
		chat.setQuestion(question);
		chat.setAnswer(answer);
		chatRepository.save(chat);

		Map<String, String> result = new HashMap<>();
		result.put("answer", answer);
		return result;
	}

	public List<Chat> getHistory(String username) {
		User user = userRepository.findByUsername(username)
				.orElseThrow(() -> new RuntimeException("用户不存在"));
		return chatRepository.findByUserIdOrderByCreateTimeDesc(user.getId());
	}
}

