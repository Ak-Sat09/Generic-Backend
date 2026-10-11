package com.example.CouponVoucherService.utils;

import com.example.CouponVoucherService.exception.BusinessException;
import com.example.CouponVoucherService.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthSupport {

    private final JwtUtils jwtUtils;

    public Long userIdFromHeader(String authorizationHeader) {
        return userIdFromToken(authorizationHeader);
    }

    public Long userIdFromToken(String token) {
        return jwtUtils.getUserIdFromToken(cleanToken(token));
    }

    public String cleanToken(String token) {
        if (token == null || token.isBlank()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED_ACTION, "Missing authorization token");
        }

        String trimmed = token.trim();
        if (trimmed.regionMatches(true, 0, "Bearer ", 0, 7)) {
            trimmed = trimmed.substring(7).trim();
        }

        if (trimmed.isBlank()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED_ACTION, "Missing authorization token");
        }
        return trimmed;
    }
}
