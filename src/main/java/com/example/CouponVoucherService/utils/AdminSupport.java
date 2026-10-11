package com.example.CouponVoucherService.utils;

import com.example.CouponVoucherService.exception.BusinessException;
import com.example.CouponVoucherService.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class AdminSupport {

    @Value("${app.admin.user-ids:1}")
    private String adminUserIds;

    public void assertAdmin(Long userId) {
        if (userId == null || !isAdmin(userId)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED_ACTION, "Admin access required");
        }
    }

    private boolean isAdmin(Long userId) {
        return Arrays.stream(adminUserIds.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .anyMatch(value -> value.equals(String.valueOf(userId)));
    }
}
