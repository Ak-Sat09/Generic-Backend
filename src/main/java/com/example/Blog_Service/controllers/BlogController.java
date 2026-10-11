package com.example.Blog_Service.controllers;

import java.util.List; 

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.Blog_Service.config.JwtUtils;
import com.example.Blog_Service.dto.CreateBlogRequest;
import com.example.Blog_Service.entity.Blog;
import com.example.Blog_Service.services.BlogServiceImpl;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/blogs")
@RequiredArgsConstructor
public class BlogController {

    private final BlogServiceImpl service;
    private final JwtUtils jwtUtils;

    @PostMapping
    public ResponseEntity<Blog> create(
            @Valid @RequestBody CreateBlogRequest request,
            @RequestHeader("Authorization") String authorizationHeader) {

        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Long userId = jwtUtils.getUserIdFromToken(authorizationHeader.substring(7).trim());
        Blog blog = service.create(request, userId);
        return ResponseEntity.ok(blog);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Blog> getBlogById(@PathVariable String id) {
        Blog blog = service.getById(id);
        return ResponseEntity.ok(blog);
    }

    @GetMapping
    public ResponseEntity<List<Blog>> getAllBlogs() {
        return ResponseEntity.ok(service.getAll());
    }
 

 


}
