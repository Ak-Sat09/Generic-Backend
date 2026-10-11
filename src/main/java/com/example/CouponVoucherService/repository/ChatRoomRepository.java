package com.example.CouponVoucherService.repository;

import com.example.CouponVoucherService.entity.ChatRoom;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ChatRoomRepository extends MongoRepository<ChatRoom, Long> {

    Optional<ChatRoom> findByCouponIdAndBuyerId(Long couponId, Long buyerId);

    List<ChatRoom> findByBuyerIdOrderByLastMessageAtDescUpdatedAtDesc(Long buyerId);

    List<ChatRoom> findBySellerIdOrderByLastMessageAtDescUpdatedAtDesc(Long sellerId);

    long countBySellerId(Long sellerId);

    long countByCouponId(Long couponId);
}
