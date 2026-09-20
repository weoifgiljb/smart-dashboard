package com.selfdiscipline.service;

import com.selfdiscipline.exception.ApiException;
import com.selfdiscipline.model.Book;
import com.selfdiscipline.model.User;
import com.selfdiscipline.model.Word;
import com.selfdiscipline.repository.BookRepository;
import com.selfdiscipline.repository.UserRepository;
import com.selfdiscipline.repository.WordRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImageServiceTest {

    @Mock
    private BookRepository bookRepository;
    @Mock
    private WordRepository wordRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ImageRateLimiter imageRateLimiter;
    @Mock
    private WebClient webClient;

    @InjectMocks
    private ImageService imageService;

    @Test
    void bookCoverRequiresUsername() {
        ApiException ex = assertThrows(ApiException.class,
                () -> imageService.generateBookCover("  ", "b1", true));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
    }

    @Test
    void existingBookCoverIsNotOverwrittenEvenWithForce() {
        Book book = new Book();
        book.setId("b1");
        book.setCover("data:image/png;base64,abc");
        when(bookRepository.findById("b1")).thenReturn(Optional.of(book));

        Book result = imageService.generateBookCover("alice", "b1", true);

        assertEquals("data:image/png;base64,abc", result.getCover());
        verify(imageRateLimiter).consumeOrThrow("user:alice");
        verify(bookRepository, never()).save(any());
        verify(webClient, never()).post();
    }

    @Test
    void unownedWordImageIsForbidden() {
        User alice = new User();
        alice.setId("u1");
        alice.setUsername("alice");
        Word word = new Word();
        word.setId("w1");
        word.setUserId(null);
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(wordRepository.findById("w1")).thenReturn(Optional.of(word));

        ApiException ex = assertThrows(ApiException.class, () -> imageService.generateWordImage("alice", "w1"));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        verify(wordRepository, never()).save(any());
        verify(webClient, never()).post();
    }

    @Test
    void otherUsersWordImageIsForbidden() {
        User alice = new User();
        alice.setId("u1");
        alice.setUsername("alice");
        Word word = new Word();
        word.setId("w1");
        word.setUserId("someone-else");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(wordRepository.findById("w1")).thenReturn(Optional.of(word));

        ApiException ex = assertThrows(ApiException.class, () -> imageService.generateWordImage("alice", "w1"));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        verify(wordRepository, never()).save(any());
    }
}
