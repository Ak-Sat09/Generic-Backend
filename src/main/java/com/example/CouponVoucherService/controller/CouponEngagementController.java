package com.example.CouponVoucherService.controller;

import com.example.CouponVoucherService.common.ApiResponse;
import com.example.CouponVoucherService.dto.request.CouponAssistantRequest;
import com.example.CouponVoucherService.dto.request.CouponVerificationRequest;
import com.example.CouponVoucherService.dto.response.CouponAssistantResponse;
import com.example.CouponVoucherService.dto.response.CouponVerificationResponse;
import com.example.CouponVoucherService.dto.response.SavedCouponResponse;
import com.example.CouponVoucherService.dto.response.SavedStateResponse;
import com.example.CouponVoucherService.service.CouponEngagementService;
import com.example.CouponVoucherService.utils.AuthSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/coupons")
@RequiredArgsConstructor
@CrossOrigin(originPatterns = "${app.cors.allowed-origin-patterns:*}")
public class CouponEngagementController {

    private final CouponEngagementService engagementService;
    private final AuthSupport authSupport;

    @GetMapping("/{couponId}/verification")
    public ApiResponse<CouponVerificationResponse> verificationStats(
            @PathVariable Long couponId,
            @RequestHeader("Authorization") String authorizationHeader) {
        Long userId = authSupport.userIdFromHeader(authorizationHeader);
        return ApiResponse.success(engagementService.verificationStats(couponId, userId));
    }

    @PostMapping("/{couponId}/verification")
    public ApiResponse<CouponVerificationResponse> vote(
            @PathVariable Long couponId,
            @RequestBody CouponVerificationRequest request,
            @RequestHeader("Authorization") String authorizationHeader) {
        Long userId = authSupport.userIdFromHeader(authorizationHeader);
        return ApiResponse.success(engagementService.vote(couponId, userId, request.getWorked()));
    }

    @GetMapping("/{couponId}/save")
    public ApiResponse<SavedStateResponse> savedState(
            @PathVariable Long couponId,
            @RequestHeader("Authorization") String authorizationHeader) {
        Long buyerId = authSupport.userIdFromHeader(authorizationHeader);
        return ApiResponse.success(engagementService.savedState(couponId, buyerId));
    }

    @PostMapping("/{couponId}/save")
    public ApiResponse<SavedStateResponse> saveCoupon(
            @PathVariable Long couponId,
            @RequestHeader("Authorization") String authorizationHeader) {
        Long buyerId = authSupport.userIdFromHeader(authorizationHeader);
        return ApiResponse.success(engagementService.saveCoupon(couponId, buyerId));
    }

    @DeleteMapping("/{couponId}/save")
    public ApiResponse<SavedStateResponse> unsaveCoupon(
            @PathVariable Long couponId,
            @RequestHeader("Authorization") String authorizationHeader) {
        Long buyerId = authSupport.userIdFromHeader(authorizationHeader);
        return ApiResponse.success(engagementService.unsaveCoupon(couponId, buyerId));
    }

    @GetMapping("/saved")
    public ApiResponse<List<SavedCouponResponse>> savedCoupons(
            @RequestHeader("Authorization") String authorizationHeader) {
        Long buyerId = authSupport.userIdFromHeader(authorizationHeader);
        return ApiResponse.success(engagementService.savedCoupons(buyerId));
    }

    @GetMapping("/reminders/expiring-soon")
    public ApiResponse<List<SavedCouponResponse>> expiringSoon(
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestParam(defaultValue = "3") int days) {
        Long buyerId = authSupport.userIdFromHeader(authorizationHeader);
        return ApiResponse.success(engagementService.expiringSoon(buyerId, days));
    }

    @PostMapping("/assistant")
    public ApiResponse<CouponAssistantResponse> assistant(
            @RequestBody CouponAssistantRequest request,
            @RequestHeader("Authorization") String authorizationHeader) {
        authSupport.userIdFromHeader(authorizationHeader);
        return ApiResponse.success(engagementService.assistantSearch(request));
    }
}
