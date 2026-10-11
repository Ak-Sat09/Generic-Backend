package com.example.CouponVoucherService.repository;

import com.example.CouponVoucherService.entity.CouponVerificationVote;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CouponVerificationVoteRepository extends MongoRepository<CouponVerificationVote, Long> {

    Optional<CouponVerificationVote> findByCouponIdAndUserId(Long couponId, Long userId);

    long countByCouponIdAndWorked(Long couponId, Boolean worked);

    long countByCouponIdInAndWorked(Collection<Long> couponIds, Boolean worked);

    List<CouponVerificationVote> findByCouponIdIn(Collection<Long> couponIds);
}
