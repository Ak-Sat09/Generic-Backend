package com.example.Blog_Service.entity;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "blog_likes")
@CompoundIndex(name = "blog_user_like_idx", def = "{'blogId': 1, 'userId': 1}", unique = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class BlogLike {

    @Id
    private String id;
    private String blogId;
    private Long userId;
    private LocalDateTime likedAt;
}
