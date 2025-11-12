package com.selfdiscipline.service;

import com.selfdiscipline.model.Book;
import com.selfdiscipline.repository.BookRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class BookService {

    @Autowired
    private BookRepository bookRepository;

    /**
     * 获取推荐书籍列表（分页）
     * 推荐算法：综合评分 + 热度权重
     */
    @Cacheable(value = "recommendedBooks", key = "#pageable.pageNumber + '_' + #pageable.pageSize + '_' + #sortBy", cacheManager = "cacheManager")
    public Page<Book> getRecommendedBooks(Pageable pageable, String sortBy) {
        List<Book> allBooks = bookRepository.findAll();
        
        // 根据排序方式计算权重分数
        List<Book> sorted = allBooks.stream()
            .filter(book -> book.getTitle() != null && !book.getTitle().isBlank())
            .sorted(getComparator(sortBy))
            .collect(Collectors.toList());
        
        // 手动分页
        int start = pageable.getPageNumber() * pageable.getPageSize();
        int end = Math.min(start + pageable.getPageSize(), sorted.size());
        List<Book> pageContent = sorted.subList(start, end);
        
        return new PageImpl<>(pageContent, pageable, sorted.size());
    }

    /**
     * 获取推荐书籍列表（兼容旧接口）
     */
    public List<Book> getRecommendedBooks() {
        return bookRepository.findTop10ByOrderByRatingDesc();
    }

    /**
     * 按分类获取书籍
     */
    @Cacheable(value = "booksByCategory", key = "#category", cacheManager = "cacheManager")
    public List<Book> getBooksByCategory(String category) {
        // 如果数据库支持分类字段，需要先添加该字段
        return bookRepository.findAll().stream()
            .filter(b -> b.getTitle() != null)
            .sorted(Comparator.comparingDouble((Book b) -> 
                b.getRating() != null ? b.getRating() : 0).reversed())
            .limit(20)
            .collect(Collectors.toList());
    }

    /**
     * 搜索书籍
     */
    public List<Book> searchBooks(String keyword) {
        String lowerKeyword = keyword.toLowerCase();
        return bookRepository.findAll().stream()
            .filter(b -> {
                String title = b.getTitle() != null ? b.getTitle().toLowerCase() : "";
                String author = b.getAuthor() != null ? b.getAuthor().toLowerCase() : "";
                String desc = b.getDescription() != null ? b.getDescription().toLowerCase() : "";
                return title.contains(lowerKeyword) || author.contains(lowerKeyword) || desc.contains(lowerKeyword);
            })
            .sorted(Comparator.comparingDouble((Book b) -> 
                b.getRating() != null ? b.getRating() : 0).reversed())
            .collect(Collectors.toList());
    }

    public Optional<Book> getBookById(String id) {
        return bookRepository.findById(id);
    }

    /**
     * 根据排序方式获取比较器
     */
    private Comparator<Book> getComparator(String sortBy) {
        if ("hot".equals(sortBy)) {
            return Comparator.comparingDouble(this::calculateHotScore).reversed();
        } else if ("new".equals(sortBy)) {
            return Comparator.comparing(Book::getId).reversed();
        } else {
            return (b1, b2) -> {
                double r1 = b1.getRating() != null ? b1.getRating() : 0;
                double r2 = b2.getRating() != null ? b2.getRating() : 0;
                return Double.compare(r2, r1);
            };
        }
    }

    /**
     * 计算热度评分（基础实现，可根据实际情况扩展）
     */
    private double calculateHotScore(Book book) {
        double rating = book.getRating() != null ? book.getRating() : 0;
        // 简单实现：使用评分作为基础
        return rating;
    }
}



