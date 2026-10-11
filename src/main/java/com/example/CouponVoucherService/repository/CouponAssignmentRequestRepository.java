package com.example.CouponVoucherService.repository;

import com.example.CouponVoucherService.entity.CouponAssignmentRequest;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface CouponAssignmentRequestRepository
        extends MongoRepository<CouponAssignmentRequest, Long> {

    List<CouponAssignmentRequest> findBySellerId(Long sellerId);

    List<CouponAssignmentRequest> findByBuyerIdOrderByCreatedAtDesc(Long buyerId);

    boolean existsByCouponIdAndBuyerIdAndStatus(Long couponId, Long buyerId, String status);

    long countBySellerId(Long sellerId);

    long countBySellerIdAndStatus(Long sellerId, String status);

    long countByCouponId(Long couponId);

    long countByCouponIdAndStatus(Long couponId, String status);
}
