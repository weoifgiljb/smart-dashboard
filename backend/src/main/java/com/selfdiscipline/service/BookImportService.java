package com.selfdiscipline.service;

import com.selfdiscipline.model.Book;
import com.selfdiscipline.repository.BookRepository;
import com.selfdiscipline.util.RemoteUrlGuard;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.io.BufferedReader;
import java.io.StringReader;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class BookImportService {

    static final String SAMPLE_SOURCE = "sample-shelf";

    @Autowired
    private BookRepository bookRepository;

    private final WebClient webClient;

    public BookImportService() {
        HttpClient httpClient = HttpClient.create().followRedirect(false);
        this.webClient = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(50 * 1024 * 1024))
                .build();
    }

    public int importSampleShelf() {
        if (bookRepository.existsBySource(SAMPLE_SOURCE)) {
            return 0;
        }
        LocalDateTime now = LocalDateTime.now();
        List<Book> samples = List.of(
                sample("深度工作", "Cal Newport", "专注", now),
                sample("原子习惯", "James Clear", "习惯", now),
                sample("心流", "Mihaly Csikszentmihalyi", "心理", now),
                sample("思考，快与慢", "Daniel Kahneman", "认知", now),
                sample("非暴力沟通", "Marshall B. Rosenberg", "沟通", now),
                sample("原则", "Ray Dalio", "决策", now),
                sample("如何阅读一本书", "Mortimer J. Adler", "阅读", now),
                sample("把时间当作朋友", "李笑来", "自律", now)
        );
        bookRepository.saveAll(samples);
        return samples.size();
    }

    @Async
    public void importBooksFromUrl(String csvUrl, int limit) {
        try {
            RemoteUrlGuard.assertSafeHttpUrl(csvUrl);
            String csvContent = webClient.get()
                    .uri(csvUrl)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (csvContent == null) {
                return;
            }

            List<Book> booksToSave = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(new StringReader(csvContent))) {
                String line;
                int lineNumber = 0;
                int imported = 0;

                while ((line = reader.readLine()) != null && imported < limit) {
                    lineNumber++;
                    if (lineNumber == 1) {
                        continue;
                    }
                    try {
                        String[] parts = parseCsvLine(line);
                        if (parts.length < 4) {
                            continue;
                        }
                        String title = cleanField(parts[1]);
                        String author = cleanField(parts[2]);
                        String category = cleanField(parts[3]);
                        String coverUrl = parts.length > 4 ? cleanField(parts[4]) : "";
                        if (title.isEmpty() || title.equalsIgnoreCase("title") || title.contains("示例")) {
                            continue;
                        }
                        Book book = new Book();
                        book.setTitle(title);
                        book.setAuthor(author.isEmpty() ? "Unknown Author" : author);
                        book.setCategory(category.isEmpty() ? "General" : category);
                        book.setCover(coverUrl.isEmpty() ? "/no-cover.svg" : coverUrl);
                        book.setDescription("From Book32 dataset - " + category);
                        book.setSource("Book32-Real");
                        book.setCreateTime(LocalDateTime.now());
                        book.setUpdateTime(LocalDateTime.now());
                        book.setRating(Math.round((Math.random() * 2 + 3) * 10) / 10.0);
                        book.setViewCount((int) (Math.random() * 1000));
                        booksToSave.add(book);
                        imported++;
                    } catch (Exception e) {
                        System.err.println("Error on line " + lineNumber + ": " + e.getMessage());
                    }
                }
            }

            if (!booksToSave.isEmpty()) {
                List<Book> oldDummyBooks = bookRepository.findAll().stream()
                        .filter(b -> "douban".equals(b.getSource()) || b.getTitle().contains("示例书籍"))
                        .toList();
                if (!oldDummyBooks.isEmpty()) {
                    bookRepository.deleteAll(oldDummyBooks);
                }
                bookRepository.saveAll(booksToSave);
            }
        } catch (Exception e) {
            System.err.println("Import failed: " + e.getMessage());
        }
    }

    private static Book sample(String title, String author, String category, LocalDateTime now) {
        Book book = new Book();
        book.setTitle(title);
        book.setAuthor(author);
        book.setCategory(category);
        book.setCover("/no-cover.svg");
        book.setDescription("示例书架 · " + category);
        book.setSource(SAMPLE_SOURCE);
        book.setRating(4.4);
        book.setViewCount(1);
        book.setCreateTime(now);
        book.setUpdateTime(now);
        return book;
    }

    private String[] parseCsvLine(String line) {
        List<String> result = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder current = new StringBuilder();
        for (char c : line.toCharArray()) {
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                result.add(current.toString());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        result.add(current.toString());
        return result.toArray(new String[0]);
    }

    private String cleanField(String field) {
        if (field == null) {
            return "";
        }
        return field.replaceAll("^\"|\"$", "").trim();
    }
}
