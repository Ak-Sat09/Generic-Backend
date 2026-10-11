package com.example.Blog_Service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class BlogResponse {
    private String id;
    private String title;
    private String content;
    private boolean canEdit;
    private boolean canRead;
    private boolean isAdmin;
}
