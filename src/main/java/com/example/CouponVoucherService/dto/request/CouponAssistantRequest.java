package com.example.CouponVoucherService.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CouponAssistantRequest {
    private String query;
    private Integer limit;
}
