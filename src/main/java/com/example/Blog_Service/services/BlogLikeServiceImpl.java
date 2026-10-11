package com.example.Blog_Service.services;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.example.Blog_Service.entity.BlogLike;
import com.example.Blog_Service.repo.BlogLikeRepo;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BlogLikeServiceImpl implements BlogLikeService {

    private final BlogLikeRepo repo;

    @Override
    public boolean toggleLike(String blogId, Long userId) {

        if (repo.existsByBlogIdAndUserId(blogId, userId)) {
            repo.deleteByBlogIdAndUserId(blogId, userId);
            return false;
        }

        BlogLike like = new BlogLike();

        like.setBlogId(blogId);
        like.setUserId(userId);
        like.setLikedAt(LocalDateTime.now());

        repo.save(like);
        return true;
    }

    public long getLikeCount(String blogId) {
        return repo.countByBlogId(blogId);
    }

}
