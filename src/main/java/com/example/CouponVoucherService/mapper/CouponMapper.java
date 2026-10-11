package com.example.CouponVoucherService.mapper;

import com.example.CouponVoucherService.dto.response.CouponResponse;
import com.example.CouponVoucherService.entity.Coupon;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CouponMapper {

    /**
     * Convert Coupon entity to CouponResponse DTO
     * @param coupon entity
     * @param showCode if true → show real coupon code, else mask it
     */
    public CouponResponse toDto(Coupon coupon, boolean showCode) {
        String code = showCode ? coupon.getCouponCode() : maskCode(coupon.getCouponCode());

        return CouponResponse.builder()
                .id(coupon.getId())
                .sellerId(coupon.getSellerId())
                .name(coupon.getName())
                .description(coupon.getDescription())
                .couponCode(code)
                .expiryDate(coupon.getExpiryDate())
                .expired(coupon.isExpired())
                .build();
    }

    /**
     * Convert list of coupons to DTO list
     */
    public List<CouponResponse> toDtoList(List<Coupon> coupons, boolean showCode) {
        return coupons.stream()
                .map(c -> toDto(c, showCode))
                .toList();
    }

    /**
     * Mask coupon code → replace all chars with '*'
     */
    private String maskCode(String code) {
        if (code == null || code.isEmpty()) return "";
        return "*".repeat(code.length());
    }
}
