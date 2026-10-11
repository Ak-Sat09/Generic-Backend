package com.example.Blog_Service.controllers;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.Blog_Service.config.JwtUtils;
import com.example.Blog_Service.dto.CommentRequest;
import com.example.Blog_Service.services.CommentService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;
    private final JwtUtils jwtUtils;

    @PostMapping("/blogs/{blogId}")
    public ResponseEntity<?> addComment(
            @PathVariable String blogId,
            @Valid @RequestBody CommentRequest request,
            @RequestHeader("Authorization") String authorizationHeader) {

        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Long userId;
        try {
            String token = authorizationHeader.substring(7).trim();
            userId = jwtUtils.getUserIdFromToken(token);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(commentService.addComment(blogId, userId, request));
    }

    @GetMapping("/blogs/{blogId}")
    public ResponseEntity<?> getComments(
            @PathVariable String blogId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return ResponseEntity.ok(
                Map.of(
                        "blogId", blogId,
                        "comments", commentService.getComments(blogId, page, size)));
    }

}
