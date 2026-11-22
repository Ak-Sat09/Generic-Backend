package com.buytogether.chat;

import com.buytogether.common.web.PageResponse;
import com.buytogether.security.AuthUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/groups/{groupId}/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MessageResponse send(@PathVariable String groupId,
                                @AuthenticationPrincipal AuthUser user,
                                @Valid @RequestBody SendMessageRequest request) {
        return messageService.send(groupId, user.id(), request.text());
    }

    @GetMapping
    public PageResponse<MessageResponse> history(@PathVariable String groupId,
                                                 @AuthenticationPrincipal AuthUser user,
                                                 @RequestParam(defaultValue = "0") int page,
                                                 @RequestParam(defaultValue = "30") int size) {
        return messageService.history(groupId, user.id(), page, size);
    }
}
