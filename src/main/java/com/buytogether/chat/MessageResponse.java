package com.buytogether.chat;

import java.time.Instant;

public record MessageResponse(String id, String senderId, String senderName, String text, Instant sentAt, boolean mine) {

    public static MessageResponse of(GroupMessage message, String viewerId) {
        return new MessageResponse(
                message.getId(),
                message.getSenderId(),
                message.getSenderName(),
                message.getText(),
                message.getSentAt(),
                message.getSenderId().equals(viewerId)
        );
    }
}
