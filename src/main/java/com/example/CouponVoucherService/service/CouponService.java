package com.example.CouponVoucherService.service;

import com.example.CouponVoucherService.dto.request.CreateCouponRequest;
import com.example.CouponVoucherService.dto.response.CouponResponse;
import com.example.CouponVoucherService.entity.Coupon;
import com.example.CouponVoucherService.entity.CouponView;
import com.example.CouponVoucherService.exception.BusinessException;
import com.example.CouponVoucherService.exception.ErrorCode;
import com.example.CouponVoucherService.mapper.CouponMapper;
import com.example.CouponVoucherService.repository.CouponAssignmentRepository;
import com.example.CouponVoucherService.repository.CouponRepo;
import com.example.CouponVoucherService.repository.CouponViewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepo couponRepository;
    private final CouponMapper couponMapper;
    private final CouponAssignmentRepository assignmentRepository;
    private final CouponViewRepository couponViewRepository;

    @Transactional
    public CouponResponse createCoupon(CreateCouponRequest request, Long sellerId) {
        validateCouponRequest(request);
        LocalDateTime now = LocalDateTime.now();

        Coupon coupon = Coupon.builder()
                .sellerId(sellerId)
                .name(request.getName().trim())
                .description(request.getDescription())
                .couponCode(request.getCouponCode().trim())
                .expiryDate(request.getExpiryDate())
                .createdAt(now)
                .updatedAt(now)
                .build();

        return couponMapper.toDto(couponRepository.save(coupon), false);
    }

    @Transactional(readOnly = true)
    public CouponResponse getCouponById(Long couponId, Long buyerId) {
        Coupon coupon = getCoupon(couponId);
        recordView(coupon, buyerId);
        boolean isAssignedToBuyer = assignmentRepository.existsByCouponIdAndBuyerId(couponId, buyerId);
        return couponMapper.toDto(coupon, isAssignedToBuyer);
    }

    @Transactional(readOnly = true)
    public List<CouponResponse> getAllCouponsForSeller(boolean showCode) {
        return couponMapper.toDtoList(couponRepository.findAll(), showCode);
    }

    @Transactional(readOnly = true)
    public List<CouponResponse> getAllCouponsBySellerId(Long sellerId, boolean showCode) {
        return couponMapper.toDtoList(couponRepository.findBySellerId(sellerId), showCode);
    }

    @Transactional
    public CouponResponse updateCoupon(Long couponId, CreateCouponRequest request, Long sellerId) {
        validateCouponRequest(request);
        Coupon coupon = getCoupon(couponId);

        if (!coupon.getSellerId().equals(sellerId)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED_ACTION, "Seller mismatch");
        }

        Coupon updated = Coupon.builder()
                .id(coupon.getId())
                .sellerId(coupon.getSellerId())
                .name(request.getName().trim())
                .description(request.getDescription())
                .couponCode(request.getCouponCode().trim())
                .expiryDate(request.getExpiryDate())
                .createdAt(coupon.getCreatedAt())
                .updatedAt(LocalDateTime.now())
                .build();

        return couponMapper.toDto(couponRepository.save(updated), false);
    }

    @Transactional
    public void deleteCoupon(Long couponId, Long sellerId) {
        Coupon coupon = getCoupon(couponId);
        if (!coupon.getSellerId().equals(sellerId)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED_ACTION, "Seller mismatch");
        }
        couponRepository.delete(coupon);
    }

    private Coupon getCoupon(Long couponId) {
        return couponRepository.findById(couponId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COUPON_NOT_FOUND, "Coupon not found"));
    }

    private void recordView(Coupon coupon, Long viewerId) {
        if (viewerId == null || coupon.getSellerId().equals(viewerId)) {
            return;
        }

        couponViewRepository.save(CouponView.builder()
                .couponId(coupon.getId())
                .viewerId(viewerId)
                .viewedAt(LocalDateTime.now())
                .build());
    }

    private void validateCouponRequest(CreateCouponRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Coupon payload is required");
        }
        if (request.getName() == null || request.getName().isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Coupon name is required");
        }
        if (request.getCouponCode() == null || request.getCouponCode().isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Coupon code is required");
        }
        if (request.getExpiryDate() == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Expiry date is required");
        }
    }
}
