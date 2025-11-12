package com.selfdiscipline.repository;

import com.selfdiscipline.model.Book;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookRepository extends MongoRepository<Book, String> {
    List<Book> findTop10ByOrderByRatingDesc();
}











