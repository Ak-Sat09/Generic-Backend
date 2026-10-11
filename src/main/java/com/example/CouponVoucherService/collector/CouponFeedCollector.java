package com.example.CouponVoucherService.collector;

import com.example.CouponVoucherService.config.CouponFeedProperties;
import com.example.CouponVoucherService.dto.event.CouponCreateEvent;
import com.example.CouponVoucherService.dto.event.ExternalCoupon;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.coupon-feed", name = "enabled", havingValue = "true")
public class CouponFeedCollector {

    private final ObjectMapper objectMapper;
    private final CouponFeedProperties properties;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Value("${app.kafka.topics.coupon-create}")
    private String couponCreateTopic;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private final Set<String> publishedKeys = ConcurrentHashMap.newKeySet();

    @Scheduled(
            fixedDelayString = "${app.coupon-feed.fixed-delay-ms:300000}",
            initialDelayString = "${app.coupon-feed.initial-delay-ms:10000}"
    )
    public void collect() {
        if (properties.getUrl() == null || properties.getUrl().isBlank()) {
            return;
        }

        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(properties.getUrl()))
                    .timeout(Duration.ofSeconds(30))
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("Coupon feed returned HTTP " + response.statusCode());
            }

            List<ExternalCoupon> coupons = objectMapper.readValue(
                    response.body(), new TypeReference<List<ExternalCoupon>>() { });

            int published = 0;
            for (ExternalCoupon coupon : coupons) {
                if (!valid(coupon)) {
                    log.warn("Skipping invalid coupon from public feed: {}", coupon.getId());
                    continue;
                }

                String key = key(coupon);
                if (!rememberKey(key)) {
                    continue;
                }

                CouponCreateEvent event = new CouponCreateEvent();
                event.setSellerId(coupon.getSellerId() != null
                        ? coupon.getSellerId() : properties.getDefaultSellerId());
                event.setName(coupon.getName());
                event.setDescription(coupon.getDescription());
                event.setCouponCode(coupon.getCouponCode());
                event.setExpiryDate(coupon.getExpiryDate());
                kafkaTemplate.send(couponCreateTopic, key,
                        objectMapper.writeValueAsString(event));
                published++;
            }
            log.info("Processed {} coupons from public feed; published {} new events", coupons.size(), published);
        } catch (Exception ex) {
            log.error("Failed to collect coupons from public feed", ex);
        }
    }

    /** Collects coupon-code offers from the documented PriceYuge/DataYuge API. */
    @Scheduled(
            fixedDelayString = "${app.coupon-feed.fixed-delay-ms:300000}",
            initialDelayString = "${app.coupon-feed.initial-delay-ms:10000}"
    )
    public void collectPriceYuge() {
        if (properties.getPriceyugeApiKey() == null || properties.getPriceyugeApiKey().isBlank()) {
            return;
        }
        try {
            StringBuilder url = new StringBuilder("https://price-api.datayuge.com/api/")
                    .append(properties.getPriceyugeApiVersion()).append("/offers/list/coupons")
                    .append("?api_key=").append(encode(properties.getPriceyugeApiKey()));
            appendQuery(url, "store_tag", properties.getPriceyugeStoreTag());
            appendQuery(url, "category_tag", properties.getPriceyugeCategoryTag());

            HttpResponse<String> response = httpClient.send(HttpRequest.newBuilder(URI.create(url.toString()))
                    .timeout(Duration.ofSeconds(30)).header("Accept", "application/json").GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("PriceYuge returned HTTP " + response.statusCode());
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode rows = root.isArray() ? root : root.path("data");
            if (!rows.isArray()) {
                rows = root.path("coupons");
            }
            List<ExternalCoupon> coupons = new ArrayList<>();
            for (JsonNode row : rows) {
                ExternalCoupon coupon = new ExternalCoupon();
                coupon.setId(text(row, "id", "coupon_id", "offer_id"));
                coupon.setName(firstText(row, "name", "title", "store_name"));
                coupon.setDescription(firstText(row, "description", "offer", "details"));
                coupon.setCouponCode(firstText(row, "couponCode", "coupon_code", "code"));
                String expiry = firstText(row, "expiryDate", "expiry_date", "end_date", "valid_till", "expires_at");
                if (expiry != null) {
                    try { coupon.setExpiryDate(parseExpiry(expiry)); }
                    catch (Exception ignored) { log.warn("Skipping PriceYuge row with invalid expiry: {}", expiry); }
                }
                coupons.add(coupon);
            }
            publish(coupons);
        } catch (Exception ex) {
            log.error("Failed to collect coupons from PriceYuge", ex);
        }
    }

    private void publish(List<ExternalCoupon> coupons) throws Exception {
        int published = 0;
        for (ExternalCoupon coupon : coupons) {
            if (!valid(coupon)) continue;
            String key = key(coupon);
            if (!rememberKey(key)) continue;
            CouponCreateEvent event = new CouponCreateEvent();
            event.setSellerId(coupon.getSellerId() != null ? coupon.getSellerId() : properties.getDefaultSellerId());
            event.setName(coupon.getName());
            event.setDescription(coupon.getDescription());
            event.setCouponCode(coupon.getCouponCode());
            event.setExpiryDate(coupon.getExpiryDate());
            kafkaTemplate.send(couponCreateTopic, key, objectMapper.writeValueAsString(event));
            published++;
        }
        log.info("Published {} new coupon events", published);
    }

    private void appendQuery(StringBuilder url, String name, String value) {
        if (value != null && !value.isBlank()) {
            url.append('&').append(name).append('=').append(encode(value));
        }
    }

    private boolean rememberKey(String key) {
        if (publishedKeys.size() >= properties.getMaxDedupeKeys()) {
            publishedKeys.clear();
        }
        return publishedKeys.add(key);
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private LocalDateTime parseExpiry(String expiry) {
        String cleaned = expiry.trim();
        try {
            return LocalDateTime.parse(cleaned);
        } catch (Exception ignored) {
            return LocalDate.parse(cleaned).atStartOfDay();
        }
    }

    private String firstText(JsonNode row, String... fields) {
        for (String field : fields) {
            String value = text(row, field);
            if (value != null && !value.isBlank()) return value;
        }
        return null;
    }

    private String text(JsonNode row, String... fields) {
        for (String field : fields) {
            JsonNode value = row.get(field);
            if (value != null && !value.isNull() && value.isValueNode()) return value.asText();
        }
        return null;
    }

    private boolean valid(ExternalCoupon coupon) {
        return coupon != null
                && coupon.getName() != null && !coupon.getName().isBlank()
                && coupon.getCouponCode() != null && !coupon.getCouponCode().isBlank()
                && coupon.getExpiryDate() != null
                && (coupon.getSellerId() != null || properties.getDefaultSellerId() > 0);
    }

    private String key(ExternalCoupon coupon) {
        if (coupon.getId() != null && !coupon.getId().isBlank()) {
            return coupon.getId();
        }
        return coupon.getCouponCode().trim().toLowerCase() + "|" + coupon.getExpiryDate();
    }
}
