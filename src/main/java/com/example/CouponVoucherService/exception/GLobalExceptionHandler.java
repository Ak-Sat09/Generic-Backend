package com.example.CouponVoucherService.exception;

import com.example.CouponVoucherService.common.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
@Slf4j
public class GLobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<?>> handleBusinessException(BusinessException ex) {
        ApiResponse<?> response = ApiResponse.failure(ex.getErrorCode().name(), ex.getMessage());
        return ResponseEntity.status(statusFor(ex.getErrorCode())).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleGenericException(Exception ex) {
        log.error("Unhandled application exception", ex);
        ApiResponse<?> response = ApiResponse.failure(ErrorCode.INTERNAL_ERROR.name(), "Unexpected server error");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    private HttpStatus statusFor(ErrorCode errorCode) {
        return switch (errorCode) {
            case COUPON_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case UNAUTHORIZED_ACTION -> HttpStatus.FORBIDDEN;
            case COUPON_ALREADY_ASSIGNED, INVALID_REQUEST -> HttpStatus.BAD_REQUEST;
            case INTERNAL_ERROR -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}
