package com.example.CouponVoucherService.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import com.example.CouponVoucherService.config.MongoIdGenerator;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Document(collection = "chat_rooms")
@CompoundIndex(name = "uk_chat_room_coupon_buyer", def = "{'couponId': 1, 'buyerId': 1}", unique = true)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class ChatRoom {

    @Id
    @Builder.Default
    private Long id = MongoIdGenerator.next();

    private Long couponId;

    private Long buyerId;

    private Long sellerId;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private LocalDateTime lastMessageAt;

    public boolean hasParticipant(Long userId) {
        return buyerId.equals(userId) || sellerId.equals(userId);
    }

    public Long getReceiverId(Long senderId) {
        if (buyerId.equals(senderId)) {
            return sellerId;
        }
        if (sellerId.equals(senderId)) {
            return buyerId;
        }
        throw new IllegalArgumentException("User is not part of this chat");
    }

    public void markMessageSent(LocalDateTime sentAt) {
        this.lastMessageAt = sentAt;
        this.updatedAt = sentAt;
    }
}
