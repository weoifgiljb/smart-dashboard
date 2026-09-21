package com.selfdiscipline.repository;

import com.selfdiscipline.model.Conversation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConversationRepository extends MongoRepository<Conversation, String> {
    List<Conversation> findByUserIdOrderByUpdatedAtDesc(String userId);

    List<Conversation> findByUserIdAndTitle(String userId, String title);
}
