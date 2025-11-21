package com.selfdiscipline.service;

import com.selfdiscipline.model.Book;
import com.selfdiscipline.repository.BookRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class BookService {

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    /**
     * 获取推荐书籍列表（分页）
     * 优化：使用数据库分页和排序
     */
    @Cacheable(value = "recommendedBooks", key = "#pageable.pageNumber + '_' + #pageable.pageSize + '_' + #sortBy", cacheManager = "cacheManager")
    public Page<Book> getRecommendedBooks(Pageable pageable, String sortBy) {
        Sort sort = Sort.by(Sort.Direction.DESC, "rating"); // Default
        
        if ("hot".equals(sortBy)) {
            sort = Sort.by(Sort.Direction.DESC, "viewCount");
        } else if ("new".equals(sortBy)) {
            sort = Sort.by(Sort.Direction.DESC, "createTime");
        } else if ("random".equals(sortBy)) {
             // Handle random discovery - Return empty page here as it's handled separately or complex to page random
             // For simplicity, if "random" is requested, we might return a simple shuffled list wrapped in Page, 
             // but Page expects total count.
             // Better approach: Client calls a separate endpoint for random, or we implement custom logic.
             // Let's fall back to "new" if random is passed to this specific method, or implement simple shuffle if page=0
             sort = Sort.by(Sort.Direction.DESC, "createTime");
        }
        
        // Reconstruct Pageable with the determined sort
        Pageable pageableWithSort = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
        
        return bookRepository.findByTitleNotNull(pageableWithSort);
    }

    /**
     * 获取推荐书籍列表（兼容旧接口）
     */
    public List<Book> getRecommendedBooks() {
        return bookRepository.findTop10ByOrderByRatingDesc();
    }

    /**
     * 随机获取推荐书籍（用于发现模式）
     */
    public List<Book> getRandomBooks(int limit) {
        Aggregation aggregation = Aggregation.newAggregation(
            Aggregation.match(Criteria.where("title").ne(null)),
            Aggregation.sample(limit)
        );
        
        AggregationResults<Book> results = mongoTemplate.aggregate(aggregation, "books", Book.class);
        return results.getMappedResults();
    }

    /**
     * 按分类获取书籍
     */
    @Cacheable(value = "booksByCategory", key = "#category", cacheManager = "cacheManager")
    public List<Book> getBooksByCategory(String category) {
        return bookRepository.findTop20ByCategoryOrderByRatingDesc(category);
    }

    /**
     * 搜索书籍
     */
    public List<Book> searchBooks(String keyword) {
        // 使用 @Query 优化的查询方法，并按评分降序排序
        return bookRepository.searchByKeyword(keyword, Sort.by(Sort.Direction.DESC, "rating"));
    }

    /**
     * 根据ID获取书籍详情并增加浏览量
     */
    public Optional<Book> getBookById(String id) {
        Optional<Book> bookOptional = bookRepository.findById(id);
        if (bookOptional.isPresent()) {
            Book book = bookOptional.get();
            // Increment view count
            book.setViewCount(book.getViewCount() == null ? 1 : book.getViewCount() + 1);
            bookRepository.save(book);
            return Optional.of(book);
        }
        return bookOptional;
    }
}
