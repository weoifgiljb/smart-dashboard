package com.selfdiscipline.service;

import com.selfdiscipline.model.Book;
import com.selfdiscipline.repository.BookRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static com.selfdiscipline.testsupport.MockitoArgs.verifyNeverSavedAll;
import static com.selfdiscipline.testsupport.MockitoArgs.verifySavedAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookImportServiceTest {

    @Mock
    private BookRepository bookRepository;

    @InjectMocks
    private BookImportService bookImportService;

    @Test
    void importSampleShelfSkipsWhenAlreadyImported() {
        when(bookRepository.existsBySource(BookImportService.SAMPLE_SOURCE)).thenReturn(true);

        assertEquals(0, bookImportService.importSampleShelf());
        verifyNeverSavedAll(bookRepository);
    }

    @Test
    void importSampleShelfSavesEightBooks() {
        when(bookRepository.existsBySource(BookImportService.SAMPLE_SOURCE)).thenReturn(false);

        assertEquals(8, bookImportService.importSampleShelf());

        List<Book> saved = verifySavedAll(bookRepository);
        assertEquals(8, saved.size());
        assertEquals("深度工作", saved.get(0).getTitle());
        assertEquals(BookImportService.SAMPLE_SOURCE, saved.get(0).getSource());
    }
}
