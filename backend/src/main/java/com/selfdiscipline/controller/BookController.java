package com.selfdiscipline.controller;

import com.selfdiscipline.model.Book;
import com.selfdiscipline.service.BookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/books")
public class BookController {

    @Autowired
    private BookService bookService;

    /**
     * 获取推荐书籍列表（分页）
     * @param page 页码（从0开始）
     * @param size 每页条数
     * @param sortBy 排序方式：rating（评分），hot（热度），new（最新）
     */
    @GetMapping
    public ResponseEntity<Page<Book>> getBooks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "rating") String sortBy) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Book> books = bookService.getRecommendedBooks(pageable, sortBy);
        return ResponseEntity.ok(books);
    }

    /**
     * 获取所有推荐书籍（不分页，兼容旧接口）
     */
    @GetMapping("/all")
    public ResponseEntity<List<Book>> getAllBooks() {
        List<Book> books = bookService.getRecommendedBooks();
        return ResponseEntity.ok(books);
    }

    /**
     * 按分类获取书籍
     */
    @GetMapping("/category/{category}")
    public ResponseEntity<List<Book>> getBooksByCategory(@PathVariable String category) {
        List<Book> books = bookService.getBooksByCategory(category);
        return ResponseEntity.ok(books);
    }

    /**
     * 搜索书籍
     */
    @GetMapping("/search")
    public ResponseEntity<List<Book>> searchBooks(@RequestParam String keyword) {
        List<Book> books = bookService.searchBooks(keyword);
        return ResponseEntity.ok(books);
    }

    /**
     * 根据ID获取书籍详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<Book> getBookById(@PathVariable("id") String id) {
        Optional<Book> book = bookService.getBookById(id);
        return book.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }
}









