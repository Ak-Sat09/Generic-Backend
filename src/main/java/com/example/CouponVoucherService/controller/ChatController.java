package com.example.CouponVoucherService.controller;

import com.example.CouponVoucherService.common.ApiResponse;
import com.example.CouponVoucherService.dto.request.SendChatMessageRequest;
import com.example.CouponVoucherService.dto.response.ChatMessageResponse;
import com.example.CouponVoucherService.dto.response.ChatRoomResponse;
import com.example.CouponVoucherService.service.ChatService;
import com.example.CouponVoucherService.utils.AuthSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/chats")
@RequiredArgsConstructor
@CrossOrigin(originPatterns = "${app.cors.allowed-origin-patterns:*}")
public class ChatController {

    private final ChatService chatService;
    private final AuthSupport authSupport;
    private final SimpMessagingTemplate messagingTemplate;

    @PostMapping("/coupons/{couponId}")
    public ApiResponse<ChatRoomResponse> createOrGetChatRoom(
            @PathVariable Long couponId,
            @RequestHeader("Authorization") String authorizationHeader) {
        Long buyerId = authSupport.userIdFromHeader(authorizationHeader);
        return ApiResponse.success(chatService.createOrGetChatRoom(couponId, buyerId));
    }

    @PostMapping("/{chatRoomId}/messages")
    public ApiResponse<ChatMessageResponse> sendMessage(
            @PathVariable Long chatRoomId,
            @RequestBody SendChatMessageRequest request,
            @RequestHeader("Authorization") String authorizationHeader) {
        Long senderId = authSupport.userIdFromHeader(authorizationHeader);
        ChatMessageResponse response = chatService.sendMessage(chatRoomId, senderId, request.getMessage());
        messagingTemplate.convertAndSend("/topic/chats/" + chatRoomId, response);
        return ApiResponse.success(response);
    }

    @PostMapping("/{chatRoomId}/media")
    public ApiResponse<ChatMessageResponse> sendMedia(
            @PathVariable Long chatRoomId,
            @RequestParam("file") MultipartFile file,
            @RequestHeader("Authorization") String authorizationHeader) {
        Long senderId = authSupport.userIdFromHeader(authorizationHeader);
        ChatMessageResponse response = chatService.sendMedia(chatRoomId, senderId, file);
        messagingTemplate.convertAndSend("/topic/chats/" + chatRoomId, response);
        return ApiResponse.success(response);
    }

    @GetMapping("/{chatRoomId}/messages")
    public ApiResponse<List<ChatMessageResponse>> getMessages(
            @PathVariable Long chatRoomId,
            @RequestHeader("Authorization") String authorizationHeader) {
        Long userId = authSupport.userIdFromHeader(authorizationHeader);
        return ApiResponse.success(chatService.getMessages(chatRoomId, userId));
    }

    @GetMapping("/buyer")
    public ApiResponse<List<ChatRoomResponse>> getBuyerChats(
            @RequestHeader("Authorization") String authorizationHeader) {
        Long buyerId = authSupport.userIdFromHeader(authorizationHeader);
        return ApiResponse.success(chatService.getBuyerChats(buyerId));
    }

    @GetMapping("/seller")
    public ApiResponse<List<ChatRoomResponse>> getSellerChats(
            @RequestHeader("Authorization") String authorizationHeader) {
        Long sellerId = authSupport.userIdFromHeader(authorizationHeader);
        return ApiResponse.success(chatService.getSellerChats(sellerId));
    }
}
