package com.example.Blog_Service.repo;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.example.Blog_Service.entity.Comment;

public interface CommentRepository extends MongoRepository<Comment, String> {

    Page<Comment> findByBlogIdOrderByCreatedAtDesc(
            String blogId,
            Pageable pageable);
}
