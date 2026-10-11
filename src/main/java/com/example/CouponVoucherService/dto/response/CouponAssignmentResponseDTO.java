package com.example.CouponVoucherService.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class CouponAssignmentResponseDTO {

    private Long requestId;
    private Long couponId;
    private String couponName;
    private Long buyerId;
    private Long sellerId;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
