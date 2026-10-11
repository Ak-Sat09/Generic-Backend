package com.example.Blog_Service.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.example.Blog_Service.dto.CommentRequest;
import com.example.Blog_Service.dto.CommentResponse;
import com.example.Blog_Service.entity.Comment;
import com.example.Blog_Service.exceptions.ResourceNotFoundException;
import com.example.Blog_Service.mapper.CommentMapper;
import com.example.Blog_Service.repo.BlogRepo;
import com.example.Blog_Service.repo.CommentRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final BlogRepo blogRepo;
    private final CommentRepository commentRepo;

    public CommentResponse addComment(String blogId, Long userId, CommentRequest request) {

        if (!blogRepo.existsById(blogId)) {
            throw new ResourceNotFoundException("Blog not found");
        }

        Comment comment = Comment.builder()
                .blogId(blogId)
                .userId(userId)
                .text(request.getText())
                .build();

        Comment savedComment = commentRepo.save(comment);

        return CommentMapper.toResponse(savedComment);
    }

    public Page<CommentResponse> getComments(String blogId, int page, int size) {

        if (!blogRepo.existsById(blogId)) {
            throw new ResourceNotFoundException("Blog not found");
        }

        Pageable pageable = PageRequest.of(page, size);

        return commentRepo
                .findByBlogIdOrderByCreatedAtDesc(blogId, pageable)
                .map(CommentMapper::toResponse);
    }
}
