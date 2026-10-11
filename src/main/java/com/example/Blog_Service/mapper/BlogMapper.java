package com.example.Blog_Service.mapper;

import org.springframework.stereotype.Component;

import com.example.Blog_Service.dto.CreateBlogRequest;
import com.example.Blog_Service.entity.Blog;

@Component
public class BlogMapper {

    public Blog toEntity(CreateBlogRequest request, Long userId) {

        Blog blog = new Blog();

        blog.setTitle(request.getTitle());
        blog.setContent(request.getContent());
        blog.setAuthorId(userId);

        return blog;
    }
}
