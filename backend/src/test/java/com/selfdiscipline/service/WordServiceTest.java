package com.selfdiscipline.service;

import com.selfdiscipline.dto.WordImportRequest;
import com.selfdiscipline.dto.WordReviewResult;
import com.selfdiscipline.exception.ApiException;
import com.selfdiscipline.model.User;
import com.selfdiscipline.model.Word;
import com.selfdiscipline.repository.UserRepository;
import com.selfdiscipline.repository.WordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.selfdiscipline.testsupport.MockitoArgs.nullableArg;
import static com.selfdiscipline.testsupport.MockitoArgs.stubSaveReturnsArg;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WordServiceTest {

    @Mock
    private WordRepository wordRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private WordService wordService;

    private User alice;

    @BeforeEach
    void setUp() {
        alice = new User();
        alice.setId("u1");
        alice.setUsername("alice");
    }

    @Test
    void knownReviewLeavesTodayQueue() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        Word word = ownedWord("w1");
        word.setReviewCount(0);
        when(wordRepository.findById("w1")).thenReturn(Optional.of(word));
        stubSaveReturnsArg(wordRepository, Word.class);

        LocalDateTime before = LocalDateTime.now();
        Word saved = wordService.reviewWord("alice", "w1", WordReviewResult.KNOWN);
        LocalDateTime after = LocalDateTime.now();
        LocalDateTime endOfToday = LocalDate.now().atTime(LocalTime.MAX);

        assertEquals("todo", saved.getStatus());
        assertEquals(1, saved.getReviewCount());
        assertTrue(saved.getDueDate().isAfter(endOfToday));
        assertTrue(!saved.getDueDate().isBefore(before.plusDays(1).minusSeconds(2)));
        assertTrue(!saved.getDueDate().isAfter(after.plusDays(1).plusSeconds(2)));
    }

    @Test
    void vagueReviewStaysDueInTenMinutes() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        Word word = ownedWord("w1");
        word.setReviewCount(2);
        when(wordRepository.findById("w1")).thenReturn(Optional.of(word));
        stubSaveReturnsArg(wordRepository, Word.class);

        LocalDateTime before = LocalDateTime.now();
        Word saved = wordService.reviewWord("alice", "w1", WordReviewResult.VAGUE);
        LocalDateTime after = LocalDateTime.now();

        assertEquals("todo", saved.getStatus());
        assertEquals(2, saved.getReviewCount());
        assertTrue(!saved.getDueDate().isBefore(before.plusMinutes(10).minusSeconds(2)));
        assertTrue(!saved.getDueDate().isAfter(after.plusMinutes(10).plusSeconds(2)));
    }

    @Test
    void unknownReviewResetsAndDueInOneMinute() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        Word word = ownedWord("w1");
        word.setReviewCount(4);
        when(wordRepository.findById("w1")).thenReturn(Optional.of(word));
        stubSaveReturnsArg(wordRepository, Word.class);

        LocalDateTime before = LocalDateTime.now();
        Word saved = wordService.reviewWord("alice", "w1", WordReviewResult.UNKNOWN);
        LocalDateTime after = LocalDateTime.now();

        assertEquals("todo", saved.getStatus());
        assertEquals(0, saved.getReviewCount());
        assertTrue(!saved.getDueDate().isBefore(before.plusMinutes(1).minusSeconds(2)));
        assertTrue(!saved.getDueDate().isAfter(after.plusMinutes(1).plusSeconds(2)));
    }

    @Test
    void reviewMarksDoneAfterFinalInterval() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        Word word = ownedWord("w1");
        word.setReviewCount(9);
        when(wordRepository.findById("w1")).thenReturn(Optional.of(word));
        stubSaveReturnsArg(wordRepository, Word.class);

        Word saved = wordService.reviewWord("alice", "w1");

        assertEquals("done", saved.getStatus());
        assertEquals(10, saved.getReviewCount());
        assertNull(saved.getDueDate());
    }

    @Test
    void reviewRejectsOtherUsersWord() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        Word word = ownedWord("w1");
        word.setUserId("other");
        when(wordRepository.findById("w1")).thenReturn(Optional.of(word));

        ApiException ex = assertThrows(ApiException.class, () -> wordService.reviewWord("alice", "w1"));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    }

    @Test
    void wordCountOnlyIncludesDone() {
        when(wordRepository.countByUserIdAndStatus("u1", "done")).thenReturn(4L);
        assertEquals(4, wordService.getWordCount("u1"));
    }

    @Test
    void todayWordsQueriesDueAndNotDone() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        Word due = ownedWord("w1");
        when(wordRepository.findByUserIdAndStatusNotAndDueDateLessThanEqualOrderByDueDateAsc(
                eq("u1"), eq("done"), nullableArg(LocalDateTime.class)
        )).thenReturn(List.of(due));

        List<Word> words = wordService.getTodayWords("alice");

        assertEquals(1, words.size());
        assertEquals("w1", words.get(0).getId());
        verify(wordRepository).findByUserIdAndStatusNotAndDueDateLessThanEqualOrderByDueDateAsc(
                eq("u1"), eq("done"), nullableArg(LocalDateTime.class)
        );
    }

    @Test
    void reviewResultRejectsInvalidToken() {
        ApiException ex = assertThrows(ApiException.class, () -> WordReviewResult.from("maybe"));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    void importWordsRejectsStartDateBeforeToday() {
        // Issue #3 Bug 6：不允许补录过去的开始日期。
        // dueDay = startDate + (sectionIndex-1)，过去的日期会让整本书一导入就处于逾期状态。
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        WordImportRequest req = new WordImportRequest();
        req.setSourceUrl("https://example.com/words.txt");
        req.setSectionSize(50);
        req.setStartDate(LocalDate.now().minusDays(1).toString());

        ApiException ex = assertThrows(ApiException.class, () -> wordService.importWords("alice", req));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertTrue(String.valueOf(ex.getMessage()).contains("不能早于今天"));
    }

    @Test
    void importDefaultWordsCarriesExampleAndPhonetic() {
        // Issue #3 Bug 4：默认词库第 3、4 列（例句、音标）必须落进实体，
        // 否则复习页的 currentWord.example / currentWord.phonetic 永远是空的。
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        List<Word> persisted = new ArrayList<>();
        when(wordRepository.saveAll(anyList())).thenAnswer(invocation -> {
            List<Word> arg = invocation.getArgument(0);
            persisted.addAll(arg);
            return arg;
        });

        Map<String, Object> resp = wordService.importDefaultWords("alice");

        assertFalse(persisted.isEmpty());
        assertEquals(persisted.size(), ((Number) resp.get("imported")).intValue());
        assertTrue(
                persisted.stream().allMatch(w -> w.getWord() != null && !w.getWord().isBlank()),
                "每个词条都必须有单词");
        assertTrue(
                persisted.stream().allMatch(w -> w.getTranslation() != null && !w.getTranslation().isBlank()),
                "每个词条都必须有翻译");
        long withExample = persisted.stream()
                .filter(w -> w.getExample() != null && !w.getExample().isBlank())
                .count();
        long withPhonetic = persisted.stream()
                .filter(w -> w.getPhonetic() != null && !w.getPhonetic().isBlank())
                .count();
        // 用等值断言而不是 > 0：只要求"至少有 1 条"的话，守不住 108/108 覆盖这个卖点。
        assertEquals(persisted.size(), withExample, "默认词库的每一条都必须带例句");
        assertEquals(persisted.size(), withPhonetic, "默认词库的每一条都必须带音标");
        // 音标在数据里存裸串，定界符由视图层添加；两边都加会渲染成 //...//。
        assertTrue(
                persisted.stream().noneMatch(w -> w.getPhonetic() != null
                        && (w.getPhonetic().startsWith("/") || w.getPhonetic().endsWith("/"))),
                "音标数据不应自带 / 定界符");
        assertTrue(
                persisted.stream().allMatch(w -> w.getSectionIndex() != null && w.getSectionIndex() > 0),
                "分区序号必须被赋值");
    }

    private static Word ownedWord(String id) {
        Word word = new Word();
        word.setId(id);
        word.setUserId("u1");
        word.setWord("apple");
        return word;
    }
}
