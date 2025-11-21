package com.selfdiscipline.service;

import com.selfdiscipline.model.Book;
import com.selfdiscipline.repository.BookRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.BufferedReader;
import java.io.StringReader;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class BookImportService {

    @Autowired
    private BookRepository bookRepository;

    private final WebClient webClient;

    public BookImportService() {
        this.webClient = WebClient.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(50 * 1024 * 1024))
                .build();
    }

    @Async
    public void importBooksFromUrl(String csvUrl, int limit) {
        try {
            System.out.println("Starting import from: " + csvUrl);
            String csvContent = webClient.get()
                    .uri(csvUrl)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (csvContent == null) {
                System.err.println("Failed to download CSV content");
                return;
            }

            List<Book> booksToSave = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(new StringReader(csvContent))) {
                String line;
                int lineNumber = 0;
                int imported = 0;
                
                while ((line = reader.readLine()) != null && imported < limit) {
                    lineNumber++;
                    
                    // Skip header
                    if (lineNumber == 1) continue;
                    
                    try {
                        // Parse CSV with proper handling of quoted fields
                        String[] parts = parseCsvLine(line);
                        
                        if (parts.length >= 4) {
                            Book book = new Book();
                            
                            // Book32 format: ASIN, Title, Author, Category, URL
                            // Example: 0001712799,"Rica la noche","Marcela Serrano","Literature & Fiction","http://ecx.images-amazon.com/images/I/512-B-1-L._SL500_.jpg"
                            
                            String title = cleanField(parts[1]);
                            String author = cleanField(parts[2]);
                            String category = cleanField(parts[3]);
                            String coverUrl = parts.length > 4 ? cleanField(parts[4]) : "";
                            
                            // Skip invalid titles or headers
                            if (title.isEmpty() || title.equalsIgnoreCase("title") || title.contains("示例")) {
                                continue;
                            }
                            
                            book.setTitle(title);
                            book.setAuthor(author.isEmpty() ? "Unknown Author" : author);
                            book.setCategory(category.isEmpty() ? "General" : category);
                            book.setCover(coverUrl.isEmpty() ? "/no-cover.svg" : coverUrl);
                            book.setDescription("From Book32 dataset - " + category);
                            book.setSource("Book32-Real");
                            book.setCreateTime(LocalDateTime.now());
                            book.setUpdateTime(LocalDateTime.now());
                            
                            // Generate random rating and view count for better UI
                            book.setRating(Math.round((Math.random() * 2 + 3) * 10) / 10.0); // 3.0-5.0
                            book.setViewCount((int)(Math.random() * 1000));
                            
                            booksToSave.add(book);
                            imported++;
                            
                            if (imported % 50 == 0) {
                                System.out.println("Imported " + imported + " books...");
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("Error on line " + lineNumber + ": " + e.getMessage());
                    }
                }
            }
            
            if (!booksToSave.isEmpty()) {
                // Only clear dummy data, keep user data if any (checking by source)
                List<Book> oldDummyBooks = bookRepository.findAll().stream()
                    .filter(b -> "douban".equals(b.getSource()) || b.getTitle().contains("示例书籍"))
                    .toList();
                if (!oldDummyBooks.isEmpty()) {
                    bookRepository.deleteAll(oldDummyBooks);
                    System.out.println("Cleared " + oldDummyBooks.size() + " dummy books.");
                }
                
                bookRepository.saveAll(booksToSave);
                System.out.println("✅ Successfully imported " + booksToSave.size() + " REAL books from Book32 dataset!");
            }

        } catch (Exception e) {
            System.err.println("❌ Import failed: " + e.getMessage());
            e.printStackTrace();
        }
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
        if (field == null) return "";
        return field.replaceAll("^\"|\"$", "").trim();
    }
}
