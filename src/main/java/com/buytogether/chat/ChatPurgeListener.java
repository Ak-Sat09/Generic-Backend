package com.buytogether.chat;

import com.buytogether.group.GroupsPurgedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class ChatPurgeListener {

    private final GroupMessageRepository messages;

    @EventListener
    void onGroupsPurged(GroupsPurgedEvent event) {
        messages.deleteByGroupIdIn(event.groupIds());
    }
}
