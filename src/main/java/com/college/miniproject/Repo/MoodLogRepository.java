// Repo/MoodLogRepository.java
package com.college.miniproject.Repo;

import com.college.miniproject.Models.MoodLog;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface MoodLogRepository extends MongoRepository<MoodLog, String> {
}