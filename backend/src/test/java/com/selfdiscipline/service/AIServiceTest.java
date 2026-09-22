package com.selfdiscipline.service;

import com.selfdiscipline.config.AliyunAIConfig;
import com.selfdiscipline.config.OllamaConfig;
import com.selfdiscipline.dto.ChatResponse;
import com.selfdiscipline.exception.ApiException;
import com.selfdiscipline.model.Chat;
import com.selfdiscipline.model.Conversation;
import com.selfdiscipline.model.User;
import com.selfdiscipline.repository.ChatRepository;
import com.selfdiscipline.repository.ConversationRepository;
import com.selfdiscipline.repository.UserRepository;
import io.netty.channel.ChannelOption;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static com.selfdiscipline.testsupport.MockitoArgs.firstArg;
import static com.selfdiscipline.testsupport.MockitoArgs.lenientWhenSave;
import static com.selfdiscipline.testsupport.MockitoArgs.nullableArg;
import static com.selfdiscipline.testsupport.MockitoArgs.stubSaveReturnsArg;
import static com.selfdiscipline.testsupport.MockitoArgs.verifyDeleted;
import static com.selfdiscipline.testsupport.MockitoArgs.verifyNeverSaved;
import static com.selfdiscipline.testsupport.MockitoArgs.verifyNeverSavedMatching;
import static com.selfdiscipline.testsupport.MockitoArgs.verifySaved;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class AIServiceTest {

    @Mock
    private ChatRepository chatRepository;
    @Mock
    private ConversationRepository conversationRepository;
    @Mock
    private UserRepository userRepository;

    private AIService aiService;
    private final AtomicInteger conversationSeq = new AtomicInteger();

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
        aiService = new AIService(chatRepository, conversationRepository, userRepository, aliyun, ollama, webClient);
        lenientWhenSave(conversationRepository, Conversation.class).thenAnswer(inv -> {
            Conversation saved = firstArg(inv, Conversation.class);
            if (saved.getId() == null) {
                saved.setId("conv-" + conversationSeq.incrementAndGet());
            }
            return saved;
        });
    }

    @Test
    void chatPersistsFallbackWhenProvidersFail() {
        User user = user("u1", "alice");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        Conversation conv = conversation("conv1", "u1", AIService.DEFAULT_TITLE);
        when(conversationRepository.findById("conv1")).thenReturn(Optional.of(conv));
        when(chatRepository.findByConversationIdAndUserIdOrderByCreateTimeAsc("conv1", "u1"))
                .thenReturn(List.of());
        stubSaveReturnsArg(chatRepository, Chat.class);

        ChatResponse res = aiService.chat("alice", "conv1", "你好");
        assertEquals("抱歉，AI服务暂时不可用，请稍后重试。", res.getAnswer());

        Chat persisted = verifySaved(chatRepository);
        assertEquals("u1", persisted.getUserId());
        assertEquals("conv1", persisted.getConversationId());
        assertEquals("你好", persisted.getQuestion());
        assertEquals(res.getAnswer(), persisted.getAnswer());
    }

    @Test
    void createConversationReusesEmptySession() {
        User user = user("u1", "alice");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        Conversation empty = conversation("empty-1", "u1", AIService.DEFAULT_TITLE);
        when(conversationRepository.findByUserIdOrderByUpdatedAtDesc("u1")).thenReturn(List.of(empty));
        when(chatRepository.existsByConversationId("empty-1")).thenReturn(false);

        Conversation first = aiService.createConversation("alice");
        Conversation second = aiService.createConversation("alice");

        assertEquals("empty-1", first.getId());
        assertEquals(first.getId(), second.getId());
        verifyNeverSaved(conversationRepository, Conversation.class);
    }

    @Test
    void createConversationCreatesWhenNoneEmpty() {
        User user = user("u1", "alice");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        Conversation filled = conversation("filled-1", "u1", "旧对话");
        when(conversationRepository.findByUserIdOrderByUpdatedAtDesc("u1")).thenReturn(List.of(filled));
        when(chatRepository.existsByConversationId("filled-1")).thenReturn(true);

        Conversation created = aiService.createConversation("alice");

        assertEquals(AIService.DEFAULT_TITLE, created.getTitle());
        assertEquals("u1", created.getUserId());
        assertNotEquals("filled-1", created.getId());
        verifySaved(conversationRepository, created);
    }

    @Test
    void listConversationsMigratesOrphanChatsIntoLegacySession() {
        User user = user("u1", "alice");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        Chat orphan = new Chat();
        orphan.setId("chat-1");
        orphan.setUserId("u1");
        orphan.setQuestion("旧问题");
        orphan.setAnswer("旧回答");
        orphan.setCreateTime(LocalDateTime.now());
        when(chatRepository.findByUserIdAndConversationIdIsNull("u1")).thenReturn(List.of(orphan));
        stubSaveReturnsArg(chatRepository, Chat.class);
        when(conversationRepository.findByUserIdOrderByUpdatedAtDesc("u1")).thenAnswer(inv -> {
            Conversation legacy = new Conversation();
            legacy.setId("conv-legacy");
            legacy.setUserId("u1");
            legacy.setTitle(AIService.LEGACY_TITLE);
            return List.of(legacy);
        });

        List<Conversation> listed = aiService.listConversations("alice");

        assertEquals("conv-1", verifySaved(chatRepository).getConversationId());
        assertTrue(listed.stream().anyMatch(c -> AIService.LEGACY_TITLE.equals(c.getTitle())));
    }

    @Test
    void listConversationsAttachesOrphansToExistingLegacySession() {
        User user = user("u1", "alice");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        Conversation existing = conversation("legacy-1", "u1", AIService.LEGACY_TITLE);
        Chat orphan = new Chat();
        orphan.setId("chat-2");
        orphan.setUserId("u1");
        orphan.setQuestion("剩余旧问题");
        orphan.setCreateTime(LocalDateTime.now());
        when(chatRepository.findByUserIdAndConversationIdIsNull("u1")).thenReturn(List.of(orphan));
        when(conversationRepository.findByUserIdAndTitle("u1", AIService.LEGACY_TITLE)).thenReturn(List.of(existing));
        stubSaveReturnsArg(chatRepository, Chat.class);
        when(conversationRepository.findByUserIdOrderByUpdatedAtDesc("u1")).thenReturn(List.of(existing));

        aiService.listConversations("alice");

        assertEquals("legacy-1", verifySaved(chatRepository).getConversationId());
        verifyNeverSavedMatching(conversationRepository, c -> c.getId() == null);
    }

    @Test
    void replaceLastUpdatesExistingChatWithoutInsert() {
        User user = user("u1", "alice");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        Conversation conv = conversation("conv1", "u1", "日程规划");
        when(conversationRepository.findById("conv1")).thenReturn(Optional.of(conv));
        Chat last = new Chat();
        last.setId("chat-last");
        last.setUserId("u1");
        last.setConversationId("conv1");
        last.setQuestion("你好");
        last.setAnswer("旧答案");
        when(chatRepository.findByConversationIdAndUserIdOrderByCreateTimeAsc("conv1", "u1"))
                .thenReturn(List.of(last));
        stubSaveReturnsArg(chatRepository, Chat.class);

        ChatResponse res = aiService.chat("alice", "conv1", "你好", true);

        assertEquals("抱歉，AI服务暂时不可用，请稍后重试。", res.getAnswer());
        Chat persisted = verifySaved(chatRepository, times(1));
        assertEquals("chat-last", persisted.getId());
        assertEquals("你好", persisted.getQuestion());
        assertEquals(res.getAnswer(), persisted.getAnswer());
    }

    @Test
    void replaceLastRejectsWhenHistoryEmpty() {
        User user = user("u1", "alice");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        Conversation conv = conversation("conv1", "u1", AIService.DEFAULT_TITLE);
        when(conversationRepository.findById("conv1")).thenReturn(Optional.of(conv));
        when(chatRepository.findByConversationIdAndUserIdOrderByCreateTimeAsc("conv1", "u1"))
                .thenReturn(List.of());

        ApiException ex = assertThrows(ApiException.class, () -> aiService.chat("alice", "conv1", "你好", true));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        verifyNeverSaved(chatRepository, Chat.class);
    }

    @Test
    void renameConversationUpdatesTitle() {
        User user = user("u1", "alice");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        Conversation conv = conversation("conv1", "u1", AIService.DEFAULT_TITLE);
        when(conversationRepository.findById("conv1")).thenReturn(Optional.of(conv));

        Conversation renamed = aiService.renameConversation("alice", "conv1", "日程规划");
        assertEquals("日程规划", renamed.getTitle());
        verifySaved(conversationRepository, conv);
    }

    @Test
    void deleteConversationRemovesChats() {
        User user = user("u1", "alice");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        Conversation conv = conversation("conv1", "u1", "要删的");
        when(conversationRepository.findById("conv1")).thenReturn(Optional.of(conv));

        aiService.deleteConversation("alice", "conv1");

        verify(chatRepository).deleteByConversationIdAndUserId("conv1", "u1");
        verifyDeleted(conversationRepository, conv);
    }

    @Test
    void chatOnOthersConversationIsForbidden() {
        User user = user("u1", "alice");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        Conversation other = conversation("conv-other", "u2", "别人的");
        when(conversationRepository.findById("conv-other")).thenReturn(Optional.of(other));

        ApiException ex = assertThrows(ApiException.class, () -> aiService.chat("alice", "conv-other", "你好"));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        verifyNeverSaved(chatRepository, Chat.class);
    }

    @Test
    void chatLoadsOnlyCurrentConversationHistory() {
        User user = user("u1", "alice");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        Conversation convB = conversation("conv-b", "u1", "B");
        when(conversationRepository.findById("conv-b")).thenReturn(Optional.of(convB));
        Chat turnB = new Chat();
        turnB.setQuestion("B问题");
        turnB.setAnswer("B回答");
        when(chatRepository.findByConversationIdAndUserIdOrderByCreateTimeAsc("conv-b", "u1"))
                .thenReturn(List.of(turnB));
        stubSaveReturnsArg(chatRepository, Chat.class);

        aiService.chat("alice", "conv-b", "追问");

        verify(chatRepository).findByConversationIdAndUserIdOrderByCreateTimeAsc("conv-b", "u1");
        verify(chatRepository, never()).findByUserIdOrderByCreateTimeDesc(nullableArg(String.class));
    }

    @Test
    void llmContextKeepsLastTenTurnsFromCurrentConversationOnly() {
        List<Chat> history = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            Chat turn = new Chat();
            turn.setQuestion("q" + i);
            turn.setAnswer("a" + i);
            history.add(turn);
        }
        Chat fromOther = new Chat();
        fromOther.setQuestion("fromA");
        fromOther.setAnswer("answerA");

        List<Map<String, String>> messages = AIService.buildLlmMessages(history, "now");

        assertEquals("q2", messages.get(0).get("content"));
        assertEquals("user", messages.get(0).get("role"));
        assertEquals("now", messages.get(messages.size() - 1).get("content"));
        assertEquals(21, messages.size());
        assertTrue(messages.stream().noneMatch(m -> "fromA".equals(m.get("content"))));
    }

    @Test
    void prepareChatStreamDoesNotPersistBeforeFinalize() {
        User user = user("u1", "alice");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        Conversation conv = conversation("conv1", "u1", AIService.DEFAULT_TITLE);
        when(conversationRepository.findById("conv1")).thenReturn(Optional.of(conv));
        when(chatRepository.findByConversationIdAndUserIdOrderByCreateTimeAsc("conv1", "u1"))
                .thenReturn(List.of());
        stubSaveReturnsArg(chatRepository, Chat.class);

        AIService.ChatStreamHandle handle = aiService.prepareChatStream("alice", "conv1", "hello", false);
        verify(chatRepository, never()).save(any(Chat.class));

        aiService.finalizeChatStream(handle, "world");
        verify(chatRepository).save(any(Chat.class));
    }

    @Test
    void llmContextPrependsRhythmSystemMessage() {
        List<Map<String, String>> messages = AIService.buildLlmMessages(List.of(), "now", "今日节律");
        assertEquals("system", messages.get(0).get("role"));
        assertEquals("今日节律", messages.get(0).get("content"));
        assertEquals("now", messages.get(messages.size() - 1).get("content"));
    }

    @Test
    void firstMessageAutoTitlesDefaultConversation() {
        User user = user("u1", "alice");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        Conversation conv = conversation("conv1", "u1", AIService.DEFAULT_TITLE);
        when(conversationRepository.findById("conv1")).thenReturn(Optional.of(conv));
        when(chatRepository.findByConversationIdAndUserIdOrderByCreateTimeAsc("conv1", "u1"))
                .thenReturn(List.of());
        stubSaveReturnsArg(chatRepository, Chat.class);

        aiService.chat("alice", "conv1", "帮我规划今天的日程以及明天的安排细节还有后天的会议");

        assertEquals("帮我规划今天的日程以及明天的安排细节还有后天的会", conv.getTitle());
        verifySaved(conversationRepository, conv);
    }

    @Test
    void listMessagesRequiresOwnership() {
        User user = user("u1", "alice");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        Conversation other = conversation("conv-other", "u2", "别人的");
        when(conversationRepository.findById("conv-other")).thenReturn(Optional.of(other));

        ApiException ex = assertThrows(ApiException.class, () -> aiService.listMessages("alice", "conv-other"));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    }

    private static User user(String id, String username) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        return user;
    }

    private static Conversation conversation(String id, String userId, String title) {
        Conversation conv = new Conversation();
        conv.setId(id);
        conv.setUserId(userId);
        conv.setTitle(title);
        return conv;
    }
}
