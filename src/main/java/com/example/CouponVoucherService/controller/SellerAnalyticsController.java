package com.example.CouponVoucherService.controller;

import com.example.CouponVoucherService.common.ApiResponse;
import com.example.CouponVoucherService.dto.response.SellerAnalyticsResponse;
import com.example.CouponVoucherService.service.CouponEngagementService;
import com.example.CouponVoucherService.utils.AuthSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/seller/analytics")
@RequiredArgsConstructor
@CrossOrigin(originPatterns = "${app.cors.allowed-origin-patterns:*}")
public class SellerAnalyticsController {

    private final CouponEngagementService engagementService;
    private final AuthSupport authSupport;

    @GetMapping
    public ApiResponse<SellerAnalyticsResponse> analytics(
            @RequestHeader("Authorization") String authorizationHeader) {
        Long sellerId = authSupport.userIdFromHeader(authorizationHeader);
        return ApiResponse.success(engagementService.sellerAnalytics(sellerId));
    }
}
