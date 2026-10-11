package com.example.CouponVoucherService.controller;

import com.example.CouponVoucherService.dto.request.ChatSocketMessageRequest;
import com.example.CouponVoucherService.dto.request.CallSignalRequest;
import com.example.CouponVoucherService.dto.response.ChatMessageResponse;
import com.example.CouponVoucherService.exception.BusinessException;
import com.example.CouponVoucherService.exception.ErrorCode;
import com.example.CouponVoucherService.service.ChatService;
import com.example.CouponVoucherService.utils.AuthSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final ChatService chatService;
    private final AuthSupport authSupport;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/chat.send")
    public void sendMessage(ChatSocketMessageRequest request) {
        Long senderId = authSupport.userIdFromToken(request.getToken());
        ChatMessageResponse response = chatService.sendMessage(
                request.getChatRoomId(),
                senderId,
                request.getMessage()
        );

        messagingTemplate.convertAndSend(
                "/topic/chats/" + request.getChatRoomId(),
                response
        );
    }

    @MessageMapping("/call.signal")
    public void signal(CallSignalRequest request) {
        if (request.getChatRoomId() == null || request.getType() == null || request.getType().isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Call signal is incomplete");
        }

        Long senderId = authSupport.userIdFromToken(request.getToken());
        chatService.assertParticipant(request.getChatRoomId(), senderId);

        Map<String, Object> signal = new HashMap<>();
        signal.put("chatRoomId", request.getChatRoomId());
        signal.put("type", request.getType());
        signal.put("data", request.getData());
        signal.put("clientId", request.getClientId());
        messagingTemplate.convertAndSend("/topic/calls/" + request.getChatRoomId(), signal);
    }
}
