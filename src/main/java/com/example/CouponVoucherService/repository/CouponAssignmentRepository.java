package com.example.CouponVoucherService.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.example.CouponVoucherService.entity.CouponAssignment;

@Repository
public interface CouponAssignmentRepository extends MongoRepository<CouponAssignment, Long> {

    // Check if a coupon is already assigned to a buyer
    boolean existsByCouponIdAndBuyerId(Long couponId, Long buyerId);

    long countBySellerId(Long sellerId);

    long countByCouponId(Long couponId);

}
