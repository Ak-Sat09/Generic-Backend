package com.example.CouponVoucherService.consumer;

import com.example.CouponVoucherService.dto.event.CouponCreateEvent;
import com.example.CouponVoucherService.dto.request.CreateCouponRequest;
import com.example.CouponVoucherService.service.CouponService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CouponCreateConsumer {

    private final ObjectMapper objectMapper;
    private final CouponService couponService;

    @KafkaListener(
            topics = "${app.kafka.topics.coupon-create}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consume(String message) {
        try {
            CouponCreateEvent event = objectMapper.readValue(message, CouponCreateEvent.class);
            validate(event);

            CreateCouponRequest request = new CreateCouponRequest();
            request.setName(event.getName());
            request.setDescription(event.getDescription());
            request.setCouponCode(event.getCouponCode());
            request.setExpiryDate(event.getExpiryDate());

            couponService.createCoupon(request, event.getSellerId());

            log.info("Coupon created from Kafka event for sellerId={}", event.getSellerId());
        } catch (Exception ex) {
            log.error("Failed to consume coupon create Kafka event. message={}", message, ex);
        }
    }

    private void validate(CouponCreateEvent event) {
        if (event.getSellerId() == null) {
            throw new IllegalArgumentException("sellerId is required");
        }
        if (event.getName() == null || event.getName().isBlank()) {
            throw new IllegalArgumentException("name is required");
        }
        if (event.getCouponCode() == null || event.getCouponCode().isBlank()) {
            throw new IllegalArgumentException("couponCode is required");
        }
        if (event.getExpiryDate() == null) {
            throw new IllegalArgumentException("expiryDate is required");
        }
    }
}
