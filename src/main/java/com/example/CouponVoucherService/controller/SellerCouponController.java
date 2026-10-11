package com.example.CouponVoucherService.controller;

import com.example.CouponVoucherService.common.ApiResponse;
import com.example.CouponVoucherService.dto.request.CreateCouponRequest;
import com.example.CouponVoucherService.dto.response.CouponResponse;
import com.example.CouponVoucherService.service.CouponService;
import com.example.CouponVoucherService.utils.AuthSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/seller/coupons")
@RequiredArgsConstructor
@CrossOrigin(originPatterns = "${app.cors.allowed-origin-patterns:*}")
public class SellerCouponController {

    private final CouponService couponService;
    private final AuthSupport authSupport;

    @PostMapping
    public ApiResponse<CouponResponse> createCoupon(
            @RequestBody CreateCouponRequest request,
            @RequestHeader("Authorization") String authorizationHeader) {
        Long sellerId = authSupport.userIdFromHeader(authorizationHeader);
        return ApiResponse.success(couponService.createCoupon(request, sellerId));
    }

    @GetMapping("/{couponId}")
    public ApiResponse<CouponResponse> getCouponById(
            @PathVariable Long couponId,
            @RequestHeader("Authorization") String authorizationHeader) {
        Long buyerId = authSupport.userIdFromHeader(authorizationHeader);
        return ApiResponse.success(couponService.getCouponById(couponId, buyerId));
    }

    @GetMapping
    public ApiResponse<List<CouponResponse>> getAllCoupons(
            @RequestParam(value = "showCode", defaultValue = "false") boolean showCode) {
        return ApiResponse.success(couponService.getAllCouponsForSeller(showCode));
    }

    @GetMapping("/my")
    public ApiResponse<List<CouponResponse>> getAllCouponsBySeller(
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestParam(defaultValue = "false") boolean showCode) {
        Long sellerId = authSupport.userIdFromHeader(authorizationHeader);
        return ApiResponse.success(couponService.getAllCouponsBySellerId(sellerId, showCode));
    }

    @PutMapping("/{id}")
    public ApiResponse<CouponResponse> updateCoupon(
            @PathVariable("id") Long couponId,
            @RequestBody CreateCouponRequest request,
            @RequestHeader("Authorization") String authorizationHeader) {
        Long sellerId = authSupport.userIdFromHeader(authorizationHeader);
        return ApiResponse.success(couponService.updateCoupon(couponId, request, sellerId));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCoupon(
            @PathVariable("id") Long couponId,
            @RequestHeader("Authorization") String authorizationHeader) {
        Long sellerId = authSupport.userIdFromHeader(authorizationHeader);
        couponService.deleteCoupon(couponId, sellerId);
    }
}
