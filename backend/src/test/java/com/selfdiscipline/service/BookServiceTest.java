package com.selfdiscipline.service;

import com.selfdiscipline.model.Book;
import com.selfdiscipline.repository.BookRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookServiceTest {

    @Mock
    private BookRepository bookRepository;
    @Mock
    private MongoTemplate mongoTemplate;

    @InjectMocks
    private BookService bookService;

    @Test
    void getBooksByIdsReturnsEmptyWhenNoIds() {
        assertTrue(bookService.getBooksByIds(List.of()).isEmpty());
    }

    @Test
    void getBooksByIdsKeepsRequestedOrder() {
        Book first = book("a");
        Book second = book("b");
        when(bookRepository.findAllById(List.of("b", "a"))).thenReturn(List.of(first, second));

        List<Book> found = bookService.getBooksByIds(List.of("b", "a"));

        assertEquals(2, found.size());
        assertEquals("b", found.get(0).getId());
        assertEquals("a", found.get(1).getId());
    }

    private static Book book(String id) {
        Book item = new Book();
        item.setId(id);
        item.setTitle(id);
        return item;
    }
}
