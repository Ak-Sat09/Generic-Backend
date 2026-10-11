package com.example.CouponVoucherService.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CallSignalRequest {
    private Long chatRoomId;
    private String type;
    private Object data;
    private String token;
    private String clientId;
}
