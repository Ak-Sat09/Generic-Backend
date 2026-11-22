package com.buytogether.group.member;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Collection;
import java.util.List;

public interface GroupMemberRepository extends MongoRepository<GroupMember, String> {

    boolean existsByGroupIdAndUserId(String groupId, String userId);

    long deleteByGroupIdAndUserId(String groupId, String userId);

    Page<GroupMember> findByGroupId(String groupId, Pageable pageable);

    Page<GroupMember> findByUserId(String userId, Pageable pageable);

    List<GroupMember> findByUserIdAndGroupIdIn(String userId, Collection<String> groupIds);

    long deleteByGroupIdIn(Collection<String> groupIds);
}
