package com.buytogether.chat;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Collection;

public interface GroupMessageRepository extends MongoRepository<GroupMessage, String> {

    Page<GroupMessage> findByGroupId(String groupId, Pageable pageable);

    long deleteByGroupIdIn(Collection<String> groupIds);
}
