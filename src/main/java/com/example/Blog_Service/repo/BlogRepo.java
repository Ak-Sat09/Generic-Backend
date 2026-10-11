package com.example.Blog_Service.repo;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.example.Blog_Service.entity.Blog;

public interface BlogRepo extends MongoRepository<Blog, String> {

}
