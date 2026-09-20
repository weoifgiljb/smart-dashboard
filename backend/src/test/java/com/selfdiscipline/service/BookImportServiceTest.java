package com.selfdiscipline.service;

import com.selfdiscipline.model.Book;
import com.selfdiscipline.repository.BookRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
        verify(bookRepository, never()).saveAll(org.mockito.ArgumentMatchers.anyList());
    }

    @Test
    void importSampleShelfSavesEightBooks() {
        when(bookRepository.existsBySource(BookImportService.SAMPLE_SOURCE)).thenReturn(false);

        assertEquals(8, bookImportService.importSampleShelf());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Book>> captor = ArgumentCaptor.forClass(List.class);
        verify(bookRepository).saveAll(captor.capture());
        assertEquals(8, captor.getValue().size());
        assertEquals("深度工作", captor.getValue().get(0).getTitle());
        assertEquals(BookImportService.SAMPLE_SOURCE, captor.getValue().get(0).getSource());
    }
}
