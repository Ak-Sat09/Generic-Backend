package com.example.CouponVoucherService.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.example.CouponVoucherService.entity.Coupon;

public interface CouponRepo extends MongoRepository<Coupon , Long>{
    // Get all coupons created by a seller
    List<Coupon> findBySellerId(Long sellerId);

    List<Coupon> findByExpiryDateAfterOrderByExpiryDateAsc(LocalDateTime now);

    List<Coupon> findByExpiryDateBetweenOrderByExpiryDateAsc(LocalDateTime start, LocalDateTime end);

    

    // Optional: get all active (not expired) coupons
    List<Coupon> findBySellerIdAndExpiryDateAfter(Long sellerId, LocalDateTime now);
}
