package com.example.CouponVoucherService.controller;

import com.example.CouponVoucherService.common.ApiResponse;
import com.example.CouponVoucherService.dto.response.CouponAssignmentResponseDTO;
import com.example.CouponVoucherService.service.CouponAssignmentRequestService;
import com.example.CouponVoucherService.utils.AuthSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/coupon-requests")
@RequiredArgsConstructor
@CrossOrigin(originPatterns = "${app.cors.allowed-origin-patterns:*}")
public class CouponAssignmentRequestController {

    private final CouponAssignmentRequestService requestService;
    private final AuthSupport authSupport;

    @PostMapping("/{couponId}")
    public ResponseEntity<ApiResponse<String>> createRequest(
            @PathVariable Long couponId,
            @RequestHeader("Authorization") String authHeader) {
        Long buyerId = authSupport.userIdFromHeader(authHeader);
        requestService.createRequest(couponId, buyerId);
        return ResponseEntity.ok(ApiResponse.success("Request sent"));
    }

    @GetMapping("/buyer")
    public ResponseEntity<ApiResponse<List<CouponAssignmentResponseDTO>>> getRequestsForBuyer(
            @RequestHeader("Authorization") String authHeader) {
        Long buyerId = authSupport.userIdFromHeader(authHeader);
        return ResponseEntity.ok(ApiResponse.success(requestService.getRequestsForBuyer(buyerId)));
    }

    @GetMapping("/seller")
    public ResponseEntity<ApiResponse<List<CouponAssignmentResponseDTO>>> getRequestsForSeller(
            @RequestHeader("Authorization") String authHeader) {
        Long sellerId = authSupport.userIdFromHeader(authHeader);
        return ResponseEntity.ok(ApiResponse.success(requestService.getRequestsForSeller(sellerId)));
    }

    @PostMapping("/seller/accept/{requestId}")
    public ResponseEntity<ApiResponse<String>> acceptRequest(
            @PathVariable Long requestId,
            @RequestHeader("Authorization") String authHeader) {
        Long sellerId = authSupport.userIdFromHeader(authHeader);
        requestService.acceptRequest(requestId, sellerId);
        return ResponseEntity.ok(ApiResponse.success("Coupon assigned successfully"));
    }

    @PostMapping("/seller/reject/{requestId}")
    public ResponseEntity<ApiResponse<String>> rejectRequest(
            @PathVariable Long requestId,
            @RequestHeader("Authorization") String authHeader) {
        Long sellerId = authSupport.userIdFromHeader(authHeader);
        requestService.rejectRequest(requestId, sellerId);
        return ResponseEntity.ok(ApiResponse.success("Coupon request rejected"));
    }
}
