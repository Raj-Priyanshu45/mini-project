package com.college.miniproject.Repo;

import com.college.miniproject.Models.UserMemory;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface UserMemoryRepository extends MongoRepository<UserMemory, String> {
}