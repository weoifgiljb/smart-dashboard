package com.selfdiscipline.repository;

import com.selfdiscipline.model.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookRepository extends MongoRepository<Book, String> {
    List<Book> findTop10ByOrderByRatingDesc();

    Page<Book> findByTitleNotNull(Pageable pageable);

    List<Book> findTop20ByCategoryOrderByRatingDesc(String category);

    @Query("{ '$or': [ { 'title': { '$regex': ?0, '$options': 'i' } }, { 'author': { '$regex': ?0, '$options': 'i' } }, { 'description': { '$regex': ?0, '$options': 'i' } } ] }")
    List<Book> searchByKeyword(String keyword, org.springframework.data.domain.Sort sort);
}
