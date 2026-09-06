// Model/Conversation.java
package com.college.miniproject.Models;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Document(collection = "conversations")
@Data
@NoArgsConstructor
public class Conversation {
    @Id
    private String userId;
    private List<ChatMessage> messages = new ArrayList<>();

    public Conversation(String userId) {
        this.userId = userId;
    }
}