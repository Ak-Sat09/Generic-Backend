package com.example.Blog_Service.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.Blog_Service.config.JwtUtils;
import com.example.Blog_Service.services.BlogLikeServiceImpl;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/likes")
@RequiredArgsConstructor
public class BlogLikeController {

    private final BlogLikeServiceImpl likeService;
    private final JwtUtils jwtUtils;

    @PostMapping("/blogs/{blogId}")
    public ResponseEntity<?> likeOrUnlike(
            @PathVariable String blogId,
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

        boolean liked = likeService.toggleLike(blogId, userId);

        return ResponseEntity.ok(
                java.util.Map.of(
                        "blogId", blogId,
                        "liked", liked));
    }

    @GetMapping("/blogs/{blogId}/count")
    public ResponseEntity<?> getLikeCount(@PathVariable String blogId) {
        return ResponseEntity.ok(
                java.util.Map.of("likes", likeService.getLikeCount(blogId)));
    }//
}
