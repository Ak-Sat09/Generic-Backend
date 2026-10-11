package com.example.Blog_Service.services;

public interface BlogLikeService {
    boolean toggleLike(String blogId, Long userId);
}
