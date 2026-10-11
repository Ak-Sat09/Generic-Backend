package com.example.CouponVoucherService.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import com.example.CouponVoucherService.config.MongoIdGenerator;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Document(collection = "chat_messages")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class ChatMessage {

    @Id
    @Builder.Default
    private Long id = MongoIdGenerator.next();

    private Long chatRoomId;

    private Long senderId;

    private Long receiverId;

    private String message;

    private String mediaUrl;

    private String mediaType;

    private LocalDateTime createdAt;
}
