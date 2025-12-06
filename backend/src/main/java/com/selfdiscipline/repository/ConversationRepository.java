package com.selfdiscipline.repository;

import com.selfdiscipline.model.Conversation;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface ConversationRepository extends MongoRepository<Conversation, String> {
    List<Conversation> findByUserIdOrderByUpdateTimeDesc(String userId);
}

