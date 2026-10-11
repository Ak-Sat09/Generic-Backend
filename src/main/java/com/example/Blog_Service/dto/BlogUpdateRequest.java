package com.example.Blog_Service.dto; 

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BlogUpdateRequest {

    private String id;
    private String title;
    private String content;

}
