package com.buytogether.group.member;

import com.buytogether.user.User;

import java.time.Instant;

public record MemberResponse(String userId, String name, MemberRole role, Instant joinedAt) {

    public static MemberResponse of(GroupMember member, User user) {
        String name = user == null ? "Member" : user.displayName();
        return new MemberResponse(member.getUserId(), name, member.getRole(), member.getJoinedAt());
    }
}
