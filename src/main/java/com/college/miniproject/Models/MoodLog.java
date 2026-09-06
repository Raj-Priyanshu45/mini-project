package com.college.miniproject.Models;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "mood_logs")
@Data
@NoArgsConstructor
public class MoodLog {
    @Id
    private String id;
    private String userId;
    private String emotion;
    private Instant timestamp;

    public MoodLog(String userId, String emotion, Instant timestamp) {
        this.userId = userId;
        this.emotion = emotion;
        this.timestamp = timestamp;
    }
}