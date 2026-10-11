package com.example.Blog_Service.repo;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.example.Blog_Service.entity.BlogLike;

public interface BlogLikeRepo extends MongoRepository<BlogLike, String> {

    boolean existsByBlogIdAndUserId(String blogId, Long userId);

    void deleteByBlogIdAndUserId(String blogId, Long userId);

    long countByBlogId(String blogId);
}
