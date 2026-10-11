package com.example.CouponVoucherService.repository;

import com.example.CouponVoucherService.entity.CouponView;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Collection;

public interface CouponViewRepository extends MongoRepository<CouponView, Long> {

    long countByCouponId(Long couponId);

    long countByCouponIdIn(Collection<Long> couponIds);
}
