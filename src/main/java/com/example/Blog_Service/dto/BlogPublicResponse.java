package com.example.Blog_Service.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class BlogPublicResponse {

    private String id;
    private String title;
    private String content;
    private Long authorId;
    private LocalDateTime createdAt;
    private String shareUrl;
}
