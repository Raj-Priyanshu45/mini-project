package com.college.miniproject.Models;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "user_memory")
@Data
@NoArgsConstructor
public class UserMemory {
    @Id
    private String userId;
    private String summary = "";
    private int messagesSinceLastSummary = 0;
}