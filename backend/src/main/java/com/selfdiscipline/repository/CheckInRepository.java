package com.selfdiscipline.repository;

import com.selfdiscipline.model.CheckIn;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface CheckInRepository extends MongoRepository<CheckIn, String> {
    Optional<CheckIn> findByUserIdAndCheckInDate(String userId, LocalDate date);
    List<CheckIn> findByUserIdOrderByCheckInDateDesc(String userId);
    boolean existsByUserIdAndCheckInDate(String userId, LocalDate date);
}











