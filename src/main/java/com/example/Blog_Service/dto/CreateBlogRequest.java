package com.example.Blog_Service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateBlogRequest {
    @NotBlank(message = "Title cannot be empty")
    @Size(max = 200)
    private String title;

    @NotBlank(message = "Content cannot be empty")
    private String content;

}
