package com.example.CouponVoucherService.controller;

import com.example.CouponVoucherService.common.ApiResponse;
import com.example.CouponVoucherService.dto.response.AdminCouponResponse;
import com.example.CouponVoucherService.service.CouponEngagementService;
import com.example.CouponVoucherService.utils.AdminSupport;
import com.example.CouponVoucherService.utils.AuthSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/coupons")
@RequiredArgsConstructor
@CrossOrigin(originPatterns = "${app.cors.allowed-origin-patterns:*}")
public class AdminCouponController {

    private final CouponEngagementService engagementService;
    private final AuthSupport authSupport;
    private final AdminSupport adminSupport;

    @GetMapping
    public ApiResponse<List<AdminCouponResponse>> coupons(
            @RequestHeader("Authorization") String authorizationHeader) {
        Long userId = authSupport.userIdFromHeader(authorizationHeader);
        adminSupport.assertAdmin(userId);
        return ApiResponse.success(engagementService.adminCoupons());
    }

    @DeleteMapping("/{couponId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeCoupon(
            @PathVariable Long couponId,
            @RequestHeader("Authorization") String authorizationHeader) {
        Long userId = authSupport.userIdFromHeader(authorizationHeader);
        adminSupport.assertAdmin(userId);
        engagementService.removeCoupon(couponId);
    }
}
