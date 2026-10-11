package com.example.CouponVoucherService.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChatSocketMessageRequest {
    private Long chatRoomId;
    private String message;
    private String token;
}
