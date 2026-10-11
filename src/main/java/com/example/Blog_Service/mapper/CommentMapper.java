package com.example.Blog_Service.mapper;

import com.example.Blog_Service.dto.CommentResponse;
import com.example.Blog_Service.entity.Comment;

public final class CommentMapper {

    private CommentMapper() {
    }

    public static CommentResponse toResponse(Comment c) {
        return new CommentResponse(
                c.getId(),
                c.getUserId(),
                c.getText(),
                c.getCreatedAt());
    }
}