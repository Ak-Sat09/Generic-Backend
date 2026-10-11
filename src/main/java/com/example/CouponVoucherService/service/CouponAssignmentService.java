package com.example.CouponVoucherService.service;

import com.example.CouponVoucherService.dto.response.CouponResponse;
import com.example.CouponVoucherService.entity.Coupon;
import com.example.CouponVoucherService.entity.CouponAssignment;
import com.example.CouponVoucherService.exception.BusinessException;
import com.example.CouponVoucherService.exception.ErrorCode;
import com.example.CouponVoucherService.mapper.CouponMapper;
import com.example.CouponVoucherService.repository.CouponAssignmentRepository;
import com.example.CouponVoucherService.repository.CouponRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime; 

@Service
@RequiredArgsConstructor
public class CouponAssignmentService {

    private final CouponRepo couponRepository;
    private final CouponAssignmentRepository assignmentRepository;
    private final CouponMapper couponMapper;
 

    @Transactional
    public CouponResponse assignCoupon(Long couponId, Long buyerId) {
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COUPON_NOT_FOUND, "Coupon not found"));

        if (coupon.getSellerId().equals(buyerId)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Seller cannot assign own coupon");
        }

        boolean exists = assignmentRepository.existsByCouponIdAndBuyerId(couponId, buyerId);
        if (exists) throw new BusinessException(ErrorCode.COUPON_ALREADY_ASSIGNED, "Coupon already assigned");

        CouponAssignment assignment = CouponAssignment.builder()
                .couponId(couponId)
                .sellerId(coupon.getSellerId())
                .buyerId(buyerId)
                .assignedAt(LocalDateTime.now())
                .build();

        assignmentRepository.save(assignment);

        return couponMapper.toDto(coupon, true);
    }

  
}
