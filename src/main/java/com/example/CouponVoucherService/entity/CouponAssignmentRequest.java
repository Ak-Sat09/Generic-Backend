package com.example.CouponVoucherService.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import com.example.CouponVoucherService.config.MongoIdGenerator;
import lombok.*;

import java.time.LocalDateTime;

@Document(collection = "coupon_assignment_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CouponAssignmentRequest {

    @Id
    @Builder.Default
    private Long id = MongoIdGenerator.next();

    private Long couponId;

    private Long buyerId;

    private Long sellerId;

    private String status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    void prePersist() {
        if (status == null) {
            status = "PENDING";
        }
        if (updatedAt == null) {
            updatedAt = createdAt;
        }
    }
}
