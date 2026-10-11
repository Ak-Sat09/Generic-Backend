package com.example.CouponVoucherService.service;

import com.example.CouponVoucherService.dto.response.CouponAssignmentResponseDTO;
import com.example.CouponVoucherService.entity.Coupon;
import com.example.CouponVoucherService.entity.CouponAssignmentRequest;
import com.example.CouponVoucherService.exception.BusinessException;
import com.example.CouponVoucherService.exception.ErrorCode;
import com.example.CouponVoucherService.repository.CouponAssignmentRepository;
import com.example.CouponVoucherService.repository.CouponAssignmentRequestRepository;
import com.example.CouponVoucherService.repository.CouponRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CouponAssignmentRequestService {

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_ACCEPTED = "ACCEPTED";
    private static final String STATUS_REJECTED = "REJECTED";

    private final CouponRepo couponRepo;
    private final CouponAssignmentRepository assignmentRepository;
    private final CouponAssignmentRequestRepository requestRepository;
    private final CouponAssignmentService assignmentService;

    @Transactional
    public void createRequest(Long couponId, Long buyerId) {
        Coupon coupon = couponRepo.findById(couponId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COUPON_NOT_FOUND, "Coupon not found"));

        if (coupon.getSellerId().equals(buyerId)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Seller cannot request own coupon");
        }

        if (assignmentRepository.existsByCouponIdAndBuyerId(couponId, buyerId)) {
            throw new BusinessException(ErrorCode.COUPON_ALREADY_ASSIGNED, "Coupon already assigned");
        }

        boolean alreadyPending =
                requestRepository.existsByCouponIdAndBuyerIdAndStatus(couponId, buyerId, STATUS_PENDING);
        if (alreadyPending) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Request already pending");
        }

        LocalDateTime now = LocalDateTime.now();
        CouponAssignmentRequest request = CouponAssignmentRequest.builder()
                .couponId(couponId)
                .buyerId(buyerId)
                .sellerId(coupon.getSellerId())
                .status(STATUS_PENDING)
                .createdAt(now)
                .updatedAt(now)
                .build();

        requestRepository.save(request);
    }

    @Transactional(readOnly = true)
    public List<CouponAssignmentResponseDTO> getRequestsForSeller(Long sellerId) {
        return toResponses(requestRepository.findBySellerId(sellerId));
    }

    @Transactional(readOnly = true)
    public List<CouponAssignmentResponseDTO> getRequestsForBuyer(Long buyerId) {
        return toResponses(requestRepository.findByBuyerIdOrderByCreatedAtDesc(buyerId));
    }

    @Transactional
    public void acceptRequest(Long requestId, Long sellerId) {
        CouponAssignmentRequest request = getRequestForSeller(requestId, sellerId);
        if (!STATUS_PENDING.equals(request.getStatus())) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Only pending requests can be accepted");
        }

        assignmentService.assignCoupon(request.getCouponId(), request.getBuyerId());

        request.setStatus(STATUS_ACCEPTED);
        request.setUpdatedAt(LocalDateTime.now());
        requestRepository.save(request);
    }

    @Transactional
    public void rejectRequest(Long requestId, Long sellerId) {
        CouponAssignmentRequest request = getRequestForSeller(requestId, sellerId);
        if (!STATUS_PENDING.equals(request.getStatus())) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Only pending requests can be rejected");
        }

        request.setStatus(STATUS_REJECTED);
        request.setUpdatedAt(LocalDateTime.now());
        requestRepository.save(request);
    }

    private CouponAssignmentRequest getRequestForSeller(Long requestId, Long sellerId) {
        CouponAssignmentRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REQUEST, "Request not found"));
        if (!request.getSellerId().equals(sellerId)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED_ACTION, "Unauthorized seller");
        }
        return request;
    }

    private List<CouponAssignmentResponseDTO> toResponses(List<CouponAssignmentRequest> requests) {
        Map<Long, String> couponNames = couponRepo.findAllById(
                        requests.stream().map(CouponAssignmentRequest::getCouponId).distinct().toList()
                )
                .stream()
                .collect(Collectors.toMap(Coupon::getId, Coupon::getName));

        return requests.stream()
                .map(req -> toResponse(req, couponNames))
                .toList();
    }

    private CouponAssignmentResponseDTO toResponse(
            CouponAssignmentRequest req,
            Map<Long, String> couponNames) {
        String couponName = couponNames.getOrDefault(req.getCouponId(), "Coupon #" + req.getCouponId());

        return CouponAssignmentResponseDTO.builder()
                .requestId(req.getId())
                .couponId(req.getCouponId())
                .couponName(couponName)
                .buyerId(req.getBuyerId())
                .sellerId(req.getSellerId())
                .status(req.getStatus())
                .createdAt(req.getCreatedAt())
                .updatedAt(req.getUpdatedAt())
                .build();
    }
}
