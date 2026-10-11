package com.example.CouponVoucherService.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ChatMessageResponse {
    private Long id;
    private Long chatRoomId;
    private Long senderId;
    private Long receiverId;
    private String message;
    private String mediaUrl;
    private String mediaType;
    private LocalDateTime createdAt;
}
