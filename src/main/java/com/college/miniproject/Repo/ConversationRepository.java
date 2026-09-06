// Repo/ConversationRepository.java
package com.college.miniproject.Repo;

import com.college.miniproject.Models.Conversation;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ConversationRepository extends MongoRepository<Conversation, String> {
}