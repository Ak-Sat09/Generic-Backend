package com.example.Blog_Service.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import com.example.Blog_Service.dto.BlogUpdateRequest;
import com.example.Blog_Service.entity.Blog;
import com.example.Blog_Service.services.BlogServiceImpl;
@Controller
@RequiredArgsConstructor
public class BlogSocketController {

    private final BlogServiceImpl service;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/edit-blog")
    public void edit(BlogUpdateRequest request) {

        Blog updated = service.update(
                request.getId(),
                request.getTitle(),
                request.getContent()
        );

        messagingTemplate.convertAndSend(
                "/topic/blog/" + updated.getId(),
                updated
        );
    }
}