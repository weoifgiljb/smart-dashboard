package com.selfdiscipline.service;

import com.selfdiscipline.config.AliyunAIConfig;
import com.selfdiscipline.config.OllamaConfig;
import com.selfdiscipline.dto.ChatResponse;
import com.selfdiscipline.model.Chat;
import com.selfdiscipline.model.User;
import com.selfdiscipline.repository.ChatRepository;
import com.selfdiscipline.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import io.netty.channel.ChannelOption;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AIServiceTest {

    @Mock
    private ChatRepository chatRepository;
    @Mock
    private UserRepository userRepository;

    private AIService aiService;

    @BeforeEach
    void setUp() {
        AliyunAIConfig aliyun = new AliyunAIConfig();
        aliyun.setApiKey("");
        aliyun.setApiUrl("http://127.0.0.1:1/qwen");
        OllamaConfig ollama = new OllamaConfig();
        ollama.setBaseUrl("http://127.0.0.1:1");
        ollama.setModel("llama3.1");
        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofMillis(300))
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 300);
        WebClient webClient = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
        aiService = new AIService(chatRepository, userRepository, aliyun, ollama, webClient);
    }

    @Test
    void chatPersistsFallbackWhenProvidersFail() {
        User user = new User();
        user.setId("u1");
        user.setUsername("alice");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(chatRepository.save(any(Chat.class))).thenAnswer(inv -> inv.getArgument(0));

        ChatResponse res = aiService.chat("alice", "你好");
        assertEquals("抱歉，AI服务暂时不可用，请稍后重试。", res.getAnswer());

        ArgumentCaptor<Chat> captor = ArgumentCaptor.forClass(Chat.class);
        verify(chatRepository).save(captor.capture());
        assertEquals("u1", captor.getValue().getUserId());
        assertEquals("你好", captor.getValue().getQuestion());
        assertEquals(res.getAnswer(), captor.getValue().getAnswer());
    }
}
