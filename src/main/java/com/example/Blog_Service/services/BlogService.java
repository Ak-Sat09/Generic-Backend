package com.example.Blog_Service.services;

import java.util.List;
 
import com.example.Blog_Service.dto.CreateBlogRequest;
import com.example.Blog_Service.entity.Blog; 

public interface BlogService {

    Blog create(CreateBlogRequest request, Long userId);

    Blog getById(String blogId);
 

    List<Blog> getAll();

    Blog update(String id, String title, String content);

}
