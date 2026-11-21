package com.buytogether.group.member;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document("group_members")
@CompoundIndexes({
        @CompoundIndex(name = "group_user_uq", def = "{'groupId': 1, 'userId': 1}", unique = true),
        @CompoundIndex(name = "user_joined_idx", def = "{'userId': 1, 'joinedAt': -1}"),
        @CompoundIndex(name = "group_joined_idx", def = "{'groupId': 1, 'joinedAt': 1}")
})
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class GroupMember {

    @Id
    private String id;

    private String groupId;
    private String userId;
    private MemberRole role;
    private Instant joinedAt;
}
