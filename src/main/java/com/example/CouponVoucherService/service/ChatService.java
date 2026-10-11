package com.example.CouponVoucherService.service;

import com.example.CouponVoucherService.dto.response.ChatMessageResponse;
import com.example.CouponVoucherService.dto.response.ChatRoomResponse;
import com.example.CouponVoucherService.entity.ChatMessage;
import com.example.CouponVoucherService.entity.ChatRoom;
import com.example.CouponVoucherService.entity.Coupon;
import com.example.CouponVoucherService.exception.BusinessException;
import com.example.CouponVoucherService.exception.ErrorCode;
import com.example.CouponVoucherService.repository.ChatMessageRepository;
import com.example.CouponVoucherService.repository.ChatRoomRepository;
import com.example.CouponVoucherService.repository.CouponRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChatService {

    private static final List<String> ALLOWED_MEDIA_PREFIXES = List.of("image/", "video/", "audio/");

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final CouponRepo couponRepository;

    @Value("${app.chat-media.directory:uploads/chat}")
    private String mediaDirectory;

    @Value("${app.chat-media.public-base-url:/uploads/chat}")
    private String mediaPublicBaseUrl;

    @Transactional
    public ChatRoomResponse createOrGetChatRoom(Long couponId, Long buyerId) {
        Coupon coupon = getCoupon(couponId);

        if (coupon.getSellerId().equals(buyerId)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Seller cannot create buyer chat for own coupon");
        }

        ChatRoom chatRoom = chatRoomRepository.findByCouponIdAndBuyerId(couponId, buyerId)
                .orElseGet(() -> {
                    LocalDateTime now = LocalDateTime.now();
                    return chatRoomRepository.save(ChatRoom.builder()
                            .couponId(coupon.getId())
                            .buyerId(buyerId)
                            .sellerId(coupon.getSellerId())
                            .createdAt(now)
                            .updatedAt(now)
                            .build());
                });

        return toRoomResponse(chatRoom);
    }

    @Transactional
    public ChatMessageResponse sendMessage(Long chatRoomId, Long senderId, String message) {
        ChatRoom chatRoom = getChatRoomForParticipant(chatRoomId, senderId);

        if (message == null || message.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Message cannot be empty");
        }

        LocalDateTime now = LocalDateTime.now();
        ChatMessage saved = chatMessageRepository.save(ChatMessage.builder()
                .chatRoomId(chatRoom.getId())
                .senderId(senderId)
                .receiverId(chatRoom.getReceiverId(senderId))
                .message(message.trim())
                .mediaUrl(null)
                .mediaType(null)
                .createdAt(now)
                .build());

        chatRoom.markMessageSent(now);
        chatRoomRepository.save(chatRoom);

        return toMessageResponse(saved);
    }

    @Transactional
    public ChatMessageResponse sendMedia(Long chatRoomId, Long senderId, MultipartFile file) {
        ChatRoom chatRoom = getChatRoomForParticipant(chatRoomId, senderId);
        String contentType = validateMedia(file);
        String mediaKind = contentType.substring(0, contentType.indexOf('/'));

        try {
            Path directory = Paths.get(mediaDirectory).toAbsolutePath().normalize();
            Files.createDirectories(directory);

            String filename = UUID.randomUUID() + sanitizeExtension(file.getOriginalFilename());
            Path target = directory.resolve(filename).normalize();
            if (!target.startsWith(directory)) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST, "Invalid media filename");
            }

            Files.copy(file.getInputStream(), target);

            LocalDateTime now = LocalDateTime.now();
            ChatMessage saved = chatMessageRepository.save(ChatMessage.builder()
                    .chatRoomId(chatRoom.getId())
                    .senderId(senderId)
                    .receiverId(chatRoom.getReceiverId(senderId))
                    .message("[" + mediaKind + "]")
                    .mediaUrl(publicMediaUrl(filename))
                    .mediaType(contentType)
                    .createdAt(now)
                    .build());
            chatRoom.markMessageSent(now);
            chatRoomRepository.save(chatRoom);
            return toMessageResponse(saved);
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Could not store media file");
        }
    }

    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getMessages(Long chatRoomId, Long userId) {
        getChatRoomForParticipant(chatRoomId, userId);
        return chatMessageRepository.findByChatRoomIdOrderByCreatedAtAsc(chatRoomId)
                .stream()
                .map(this::toMessageResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ChatRoomResponse> getBuyerChats(Long buyerId) {
        return chatRoomRepository.findByBuyerIdOrderByLastMessageAtDescUpdatedAtDesc(buyerId)
                .stream()
                .map(this::toRoomResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ChatRoomResponse> getSellerChats(Long sellerId) {
        return chatRoomRepository.findBySellerIdOrderByLastMessageAtDescUpdatedAtDesc(sellerId)
                .stream()
                .map(this::toRoomResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public void assertParticipant(Long chatRoomId, Long userId) {
        getChatRoomForParticipant(chatRoomId, userId);
    }

    private ChatRoom getChatRoomForParticipant(Long chatRoomId, Long userId) {
        if (chatRoomId == null || userId == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Chat room and user are required");
        }

        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REQUEST, "Chat room not found"));

        if (!chatRoom.hasParticipant(userId)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED_ACTION, "User is not part of this chat");
        }

        return chatRoom;
    }

    private String validateMedia(MultipartFile file) {
        if (file == null || file.isEmpty() || file.getContentType() == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Media file is required");
        }

        String contentType = file.getContentType().toLowerCase(Locale.ROOT);
        boolean allowed = ALLOWED_MEDIA_PREFIXES.stream().anyMatch(contentType::startsWith);
        if (!allowed) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Only image, video, and audio files are allowed");
        }

        return contentType;
    }

    private String sanitizeExtension(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return "";
        }

        String filename = originalFilename.replace('\\', '/');
        filename = filename.substring(filename.lastIndexOf('/') + 1);
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            return "";
        }

        String extension = filename.substring(dot).toLowerCase(Locale.ROOT);
        return extension.matches("\\.[a-z0-9]{1,12}") ? extension : "";
    }

    private String publicMediaUrl(String filename) {
        String baseUrl = mediaPublicBaseUrl == null || mediaPublicBaseUrl.isBlank()
                ? "/uploads/chat"
                : mediaPublicBaseUrl.trim();
        return baseUrl.replaceAll("/+$", "") + "/" + filename;
    }

    private Coupon getCoupon(Long couponId) {
        return couponRepository.findById(couponId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COUPON_NOT_FOUND, "Coupon not found"));
    }

    private ChatRoomResponse toRoomResponse(ChatRoom chatRoom) {
        Coupon coupon = getCoupon(chatRoom.getCouponId());
        String lastMessage = chatMessageRepository.findTopByChatRoomIdOrderByCreatedAtDesc(chatRoom.getId())
                .map(ChatMessage::getMessage)
                .orElse(null);

        return ChatRoomResponse.builder()
                .id(chatRoom.getId())
                .couponId(chatRoom.getCouponId())
                .couponName(coupon.getName())
                .buyerId(chatRoom.getBuyerId())
                .sellerId(chatRoom.getSellerId())
                .lastMessage(lastMessage)
                .lastMessageAt(chatRoom.getLastMessageAt())
                .createdAt(chatRoom.getCreatedAt())
                .build();
    }

    private ChatMessageResponse toMessageResponse(ChatMessage message) {
        return ChatMessageResponse.builder()
                .id(message.getId())
                .chatRoomId(message.getChatRoomId())
                .senderId(message.getSenderId())
                .receiverId(message.getReceiverId())
                .message(message.getMessage())
                .mediaUrl(message.getMediaUrl())
                .mediaType(message.getMediaType())
                .createdAt(message.getCreatedAt())
                .build();
    }
}
