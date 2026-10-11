package com.example.CouponVoucherService.repository;

import com.example.CouponVoucherService.entity.SavedCoupon;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SavedCouponRepository extends MongoRepository<SavedCoupon, Long> {

    Optional<SavedCoupon> findByBuyerIdAndCouponId(Long buyerId, Long couponId);

    boolean existsByBuyerIdAndCouponId(Long buyerId, Long couponId);

    long countByCouponId(Long couponId);

    long countByCouponIdIn(Collection<Long> couponIds);

    void deleteByBuyerIdAndCouponId(Long buyerId, Long couponId);

    List<SavedCoupon> findByBuyerIdOrderByCreatedAtDesc(Long buyerId);
}
