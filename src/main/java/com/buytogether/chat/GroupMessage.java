package com.buytogether.chat;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document("bt_group_messages")
@CompoundIndex(name = "group_sent_idx", def = "{'groupId': 1, 'sentAt': -1}")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class GroupMessage {

    @Id
    private String id;

    private String groupId;
    private String senderId;

    // Copied at send time so reading a chat never needs a user lookup per message
    private String senderName;

    private String text;
    private Instant sentAt;
}
