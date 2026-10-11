package com.example.CouponVoucherService.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import com.example.CouponVoucherService.config.MongoIdGenerator;
import lombok.*;

import java.time.LocalDateTime;

@Document(collection = "coupon_assignments")
@CompoundIndex(name = "uk_coupon_assignment_coupon_buyer", def = "{'couponId': 1, 'buyerId': 1}", unique = true)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class CouponAssignment {

    @Id
    @Builder.Default
    private Long id = MongoIdGenerator.next();

    private Long couponId;

    private Long sellerId;

    private Long buyerId;

    private LocalDateTime assignedAt;

    /* ===== Domain Method ===== */
    
    public boolean isAssignedTo(Long buyerId) {
        return this.buyerId.equals(buyerId);
    }
}
