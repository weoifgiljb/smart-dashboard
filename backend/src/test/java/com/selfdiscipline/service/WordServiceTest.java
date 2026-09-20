package com.selfdiscipline.service;

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
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
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
        when(wordRepository.save(any(Word.class))).thenAnswer(inv -> inv.getArgument(0));

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
        when(wordRepository.save(any(Word.class))).thenAnswer(inv -> inv.getArgument(0));

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
        when(wordRepository.save(any(Word.class))).thenAnswer(inv -> inv.getArgument(0));

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
        when(wordRepository.save(any(Word.class))).thenAnswer(inv -> inv.getArgument(0));

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
                eq("u1"), eq("done"), any()
        )).thenReturn(List.of(due));

        List<Word> words = wordService.getTodayWords("alice");

        assertEquals(1, words.size());
        assertEquals("w1", words.get(0).getId());
        verify(wordRepository).findByUserIdAndStatusNotAndDueDateLessThanEqualOrderByDueDateAsc(
                eq("u1"), eq("done"), any()
        );
    }

    @Test
    void reviewResultRejectsInvalidToken() {
        ApiException ex = assertThrows(ApiException.class, () -> WordReviewResult.from("maybe"));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    private static Word ownedWord(String id) {
        Word word = new Word();
        word.setId(id);
        word.setUserId("u1");
        word.setWord("apple");
        return word;
    }
}
