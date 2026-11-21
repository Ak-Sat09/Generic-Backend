package com.buytogether.chat;

import com.buytogether.common.web.PageResponse;
import com.buytogether.common.web.Paging;
import com.buytogether.group.GroupQueryService;
import com.buytogether.user.User;
import com.buytogether.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.Clock;

@Service
@RequiredArgsConstructor
public class MessageService {

    private static final Sort NEWEST_FIRST = Sort.by(Sort.Direction.DESC, "sentAt");

    private final GroupMessageRepository messages;
    private final GroupQueryService groupQueries;
    private final UserService userService;
    private final Clock clock;

    public MessageResponse send(String groupId, String senderId, String text) {
        groupQueries.requireActiveMember(groupId, senderId);
        User sender = userService.get(senderId);

        GroupMessage message = messages.save(GroupMessage.builder()
                .groupId(groupId)
                .senderId(senderId)
                .senderName(sender.displayName())
                .text(text.strip())
                .sentAt(clock.instant())
                .build());
        return MessageResponse.of(message, senderId);
    }

    /** Newest first, so page 0 is what a chat screen shows on open. */
    public PageResponse<MessageResponse> history(String groupId, String viewerId, int page, int size) {
        groupQueries.requireMember(groupId, viewerId);
        return PageResponse.from(
                messages.findByGroupId(groupId, Paging.of(page, size, NEWEST_FIRST)),
                message -> MessageResponse.of(message, viewerId));
    }
}
