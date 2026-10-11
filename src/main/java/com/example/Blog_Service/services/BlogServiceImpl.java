package com.example.Blog_Service.services;

import java.util.List;

import org.springframework.stereotype.Service;
 
import com.example.Blog_Service.dto.CreateBlogRequest;
import com.example.Blog_Service.entity.Blog; 
import com.example.Blog_Service.mapper.BlogMapper; 
import com.example.Blog_Service.repo.BlogRepo;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BlogServiceImpl implements BlogService {

    private final BlogRepo repo;
    private final BlogMapper mapper;  

    @Override
    public Blog create(CreateBlogRequest request, Long userId) {
        Blog blog = mapper.toEntity(request, userId);
        return repo.save(blog);
    }

    @Override
    public Blog getById(String blogId) {
        return repo.findById(blogId)
                .orElseThrow(() -> new RuntimeException("Blog not found"));
    }
 

    @Override
    public List<Blog> getAll() {
        return repo.findAll();
    } 
 

    public Blog update(String id, String title, String content) {

        Blog blog = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Not found"));

        blog.setTitle(title);
        blog.setContent(content);

        return repo.save(blog);  
    }


    
    
}
