package com.example.CouponVoucherService.service;

import com.example.CouponVoucherService.dto.request.CouponAssistantRequest;
import com.example.CouponVoucherService.dto.response.AdminCouponResponse;
import com.example.CouponVoucherService.dto.response.CouponAssistantResponse; 
import com.example.CouponVoucherService.dto.response.CouponVerificationResponse;
import com.example.CouponVoucherService.dto.response.SavedCouponResponse;
import com.example.CouponVoucherService.dto.response.SavedStateResponse;
import com.example.CouponVoucherService.dto.response.SellerAnalyticsResponse;
import com.example.CouponVoucherService.entity.Coupon;
import com.example.CouponVoucherService.entity.CouponVerificationVote;
import com.example.CouponVoucherService.entity.SavedCoupon;
import com.example.CouponVoucherService.exception.BusinessException;
import com.example.CouponVoucherService.exception.ErrorCode;
import com.example.CouponVoucherService.mapper.CouponMapper;
import com.example.CouponVoucherService.repository.ChatRoomRepository;
import com.example.CouponVoucherService.repository.CouponAssignmentRepository;
import com.example.CouponVoucherService.repository.CouponAssignmentRequestRepository;
import com.example.CouponVoucherService.repository.CouponRepo;
import com.example.CouponVoucherService.repository.CouponVerificationVoteRepository;
import com.example.CouponVoucherService.repository.CouponViewRepository;
import com.example.CouponVoucherService.repository.SavedCouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CouponEngagementService {

    private static final String STATUS_ACCEPTED = "ACCEPTED";
    private static final int DEFAULT_ASSISTANT_LIMIT = 5;
    private static final Set<String> ASSISTANT_STOP_WORDS = Set.of(
            "a", "an", "and", "best", "coupon", "coupons", "day", "days", "deal", "deals",
            "discount", "expiry", "expire", "expiring", "find", "for", "in", "me", "next",
            "of", "off", "soon", "the", "to", "under", "with", "within"
    );

    private final CouponRepo couponRepository;
    private final CouponMapper couponMapper;
    private final CouponVerificationVoteRepository voteRepository;
    private final SavedCouponRepository savedCouponRepository;
    private final CouponViewRepository couponViewRepository;
    private final CouponAssignmentRequestRepository requestRepository;
    private final CouponAssignmentRepository assignmentRepository;
    private final ChatRoomRepository chatRoomRepository;

    @Transactional
    public CouponVerificationResponse vote(Long couponId, Long userId, Boolean worked) {
        if (worked == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Vote value is required");
        }
        Coupon coupon = getCoupon(couponId);
        if (coupon.getSellerId().equals(userId)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Seller cannot verify own coupon");
        }

        LocalDateTime now = LocalDateTime.now();
        CouponVerificationVote vote = voteRepository.findByCouponIdAndUserId(couponId, userId)
                .orElseGet(() -> CouponVerificationVote.builder()
                        .couponId(couponId)
                        .userId(userId)
                        .createdAt(now)
                        .build());
        vote.setWorked(worked);
        vote.setUpdatedAt(now);
        voteRepository.save(vote);

        return verificationStats(couponId, userId);
    }

    @Transactional(readOnly = true)
    public CouponVerificationResponse verificationStats(Long couponId, Long userId) {
        getCoupon(couponId);
        long worked = voteRepository.countByCouponIdAndWorked(couponId, true);
        long notWorked = voteRepository.countByCouponIdAndWorked(couponId, false);
        Boolean myVote = voteRepository.findByCouponIdAndUserId(couponId, userId)
                .map(CouponVerificationVote::getWorked)
                .orElse(null);

        return CouponVerificationResponse.builder()
                .couponId(couponId)
                .workedCount(worked)
                .notWorkedCount(notWorked)
                .verificationScore(score(worked, notWorked))
                .myVote(myVote)
                .build();
    }

    @Transactional
    public SavedStateResponse saveCoupon(Long couponId, Long buyerId) {
        Coupon coupon = getCoupon(couponId);
        if (coupon.getSellerId().equals(buyerId)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Seller cannot save own coupon");
        }

        boolean exists = savedCouponRepository.existsByBuyerIdAndCouponId(buyerId, couponId);
        if (!exists) {
            savedCouponRepository.save(SavedCoupon.builder()
                    .buyerId(buyerId)
                    .couponId(couponId)
                    .createdAt(LocalDateTime.now())
                    .build());
        }
        return SavedStateResponse.builder().couponId(couponId).saved(true).build();
    }

    @Transactional
    public SavedStateResponse unsaveCoupon(Long couponId, Long buyerId) {
        savedCouponRepository.deleteByBuyerIdAndCouponId(buyerId, couponId);
        return SavedStateResponse.builder().couponId(couponId).saved(false).build();
    }

    @Transactional(readOnly = true)
    public SavedStateResponse savedState(Long couponId, Long buyerId) {
        getCoupon(couponId);
        boolean saved = savedCouponRepository.existsByBuyerIdAndCouponId(buyerId, couponId);
        return SavedStateResponse.builder().couponId(couponId).saved(saved).build();
    }

    @Transactional(readOnly = true)
    public List<SavedCouponResponse> savedCoupons(Long buyerId) {
        return savedCouponRows(savedCouponRepository.findByBuyerIdOrderByCreatedAtDesc(buyerId));
    }

    @Transactional(readOnly = true)
    public List<SavedCouponResponse> expiringSoon(Long buyerId, int days) {
        int safeDays = Math.max(1, Math.min(days, 30));
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime end = now.plusDays(safeDays);

        return savedCouponRows(savedCouponRepository.findByBuyerIdOrderByCreatedAtDesc(buyerId))
                .stream()
                .filter(row -> {
                    LocalDateTime expiry = row.getCoupon().getExpiryDate();
                    return expiry != null && !expiry.isBefore(now) && !expiry.isAfter(end);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public SellerAnalyticsResponse sellerAnalytics(Long sellerId) {
        List<Coupon> coupons = couponRepository.findBySellerId(sellerId);
        List<Long> couponIds = coupons.stream().map(Coupon::getId).toList();

        long workedCount = couponIds.isEmpty() ? 0 : voteRepository.countByCouponIdInAndWorked(couponIds, true);
        long notWorkedCount = couponIds.isEmpty() ? 0 : voteRepository.countByCouponIdInAndWorked(couponIds, false);
        long viewCount = couponIds.isEmpty() ? 0 : couponViewRepository.countByCouponIdIn(couponIds);

        List<SellerAnalyticsResponse.CouponAnalytics> couponRows = coupons.stream()
                .map(coupon -> {
                    long worked = voteRepository.countByCouponIdAndWorked(coupon.getId(), true);
                    long notWorked = voteRepository.countByCouponIdAndWorked(coupon.getId(), false);
                    return SellerAnalyticsResponse.CouponAnalytics.builder()
                            .couponId(coupon.getId())
                            .couponName(coupon.getName())
                            .viewCount(couponViewRepository.countByCouponId(coupon.getId()))
                            .requestCount(requestRepository.countByCouponId(coupon.getId()))
                            .acceptedRequestCount(requestRepository.countByCouponIdAndStatus(coupon.getId(), STATUS_ACCEPTED))
                            .chatCount(chatRoomRepository.countByCouponId(coupon.getId()))
                            .assignedCount(assignmentRepository.countByCouponId(coupon.getId()))
                            .workedCount(worked)
                            .notWorkedCount(notWorked)
                            .verificationScore(score(worked, notWorked))
                            .build();
                })
                .toList();

        return SellerAnalyticsResponse.builder()
                .couponCount(coupons.size())
                .requestCount(requestRepository.countBySellerId(sellerId))
                .acceptedRequestCount(requestRepository.countBySellerIdAndStatus(sellerId, STATUS_ACCEPTED))
                .chatCount(chatRoomRepository.countBySellerId(sellerId))
                .assignedCount(assignmentRepository.countBySellerId(sellerId))
                .viewCount(viewCount)
                .workedCount(workedCount)
                .notWorkedCount(notWorkedCount)
                .coupons(couponRows)
                .build();
    }

    @Transactional(readOnly = true)
    public List<AdminCouponResponse> adminCoupons() {
        return couponRepository.findAll()
                .stream()
                .map(this::toAdminCoupon)
                .sorted(Comparator.comparing(AdminCouponResponse::getRiskLevel).reversed())
                .toList();
    }

    @Transactional
    public void removeCoupon(Long couponId) {
        couponRepository.delete(getCoupon(couponId));
    }

    @Transactional(readOnly = true)
    public CouponAssistantResponse assistantSearch(CouponAssistantRequest request) {
        String query = request == null || request.getQuery() == null || request.getQuery().isBlank()
                ? "best active coupons expiring soon"
                : request.getQuery().trim();
        int limit = request == null || request.getLimit() == null
                ? DEFAULT_ASSISTANT_LIMIT
                : Math.max(1, Math.min(request.getLimit(), 10));
        Integer maxDays = extractMaxDays(query);
        List<String> terms = queryTerms(query);
        LocalDateTime now = LocalDateTime.now();

        List<Coupon> activeCoupons = couponRepository.findByExpiryDateAfterOrderByExpiryDateAsc(now);
        List<Coupon> filtered = activeCoupons.stream()
                .filter(coupon -> maxDays == null || !coupon.getExpiryDate().isAfter(now.plusDays(maxDays)))
                .filter(coupon -> terms.isEmpty() || matchesAnyTerm(coupon, terms))
                .sorted(Comparator.comparingInt((Coupon coupon) -> assistantRank(coupon, terms, now)).reversed())
                .limit(limit)
                .toList();

        if (filtered.isEmpty() && !terms.isEmpty()) {
            filtered = activeCoupons.stream()
                    .filter(coupon -> maxDays == null || !coupon.getExpiryDate().isAfter(now.plusDays(maxDays)))
                    .sorted(Comparator.comparingInt((Coupon coupon) -> assistantRank(coupon, List.of(), now)).reversed())
                    .limit(limit)
                    .toList();
        }

        List<CouponAssistantResponse.AssistantCoupon> matches = filtered.stream()
                .map(coupon -> toAssistantCoupon(coupon, terms, now))
                .toList();

        String filter = "active coupons"
                + (maxDays == null ? "" : " expiring within " + maxDays + " day(s)")
                + (terms.isEmpty() ? "" : " matching " + String.join(", ", terms));
        return CouponAssistantResponse.builder()
                .query(query)
                .interpretedFilter(filter)
                .coupons(matches)
                .build();
    }

    private List<SavedCouponResponse> savedCouponRows(List<SavedCoupon> savedRows) {
        Map<Long, Coupon> couponsById = couponRepository.findAllById(
                        savedRows.stream().map(SavedCoupon::getCouponId).distinct().toList()
                )
                .stream()
                .collect(Collectors.toMap(Coupon::getId, Function.identity()));

        return savedRows.stream()
                .map(saved -> {
                    Coupon coupon = couponsById.get(saved.getCouponId());
                    if (coupon == null) return null;
                    return SavedCouponResponse.builder()
                            .coupon(couponMapper.toDto(coupon, false))
                            .savedAt(saved.getCreatedAt())
                            .daysUntilExpiry(daysUntilExpiry(coupon))
                            .build();
                })
                .filter(row -> row != null)
                .toList();
    }

    private AdminCouponResponse toAdminCoupon(Coupon coupon) {
        long worked = voteRepository.countByCouponIdAndWorked(coupon.getId(), true);
        long notWorked = voteRepository.countByCouponIdAndWorked(coupon.getId(), false);
        return AdminCouponResponse.builder()
                .id(coupon.getId())
                .sellerId(coupon.getSellerId())
                .name(coupon.getName())
                .description(coupon.getDescription())
                .couponCode(coupon.getCouponCode())
                .expiryDate(coupon.getExpiryDate())
                .expired(coupon.isExpired())
                .viewCount(couponViewRepository.countByCouponId(coupon.getId()))
                .savedCount(savedCouponRepository.countByCouponId(coupon.getId()))
                .requestCount(requestRepository.countByCouponId(coupon.getId()))
                .workedCount(worked)
                .notWorkedCount(notWorked)
                .verificationScore(score(worked, notWorked))
                .riskLevel(riskLevel(coupon, worked, notWorked))
                .build();
    }

    private CouponAssistantResponse.AssistantCoupon toAssistantCoupon(
            Coupon coupon,
            List<String> terms,
            LocalDateTime now) {
        long worked = voteRepository.countByCouponIdAndWorked(coupon.getId(), true);
        long notWorked = voteRepository.countByCouponIdAndWorked(coupon.getId(), false);
        long days = Math.max(0, ChronoUnit.DAYS.between(now.toLocalDate(), coupon.getExpiryDate().toLocalDate()));
        int score = score(worked, notWorked);

        return CouponAssistantResponse.AssistantCoupon.builder()
                .id(coupon.getId())
                .sellerId(coupon.getSellerId())
                .name(coupon.getName())
                .description(coupon.getDescription())
                .couponCode(coupon.getCouponCode())
                .expiryDate(coupon.getExpiryDate())
                .verificationScore(score)
                .workedCount(worked)
                .notWorkedCount(notWorked)
                .reason(reason(coupon, terms, days, score))
                .build();
    }

    private int assistantRank(Coupon coupon, List<String> terms, LocalDateTime now) {
        long worked = voteRepository.countByCouponIdAndWorked(coupon.getId(), true);
        long notWorked = voteRepository.countByCouponIdAndWorked(coupon.getId(), false);
        int termScore = terms.stream().mapToInt(term -> searchableText(coupon).contains(term) ? 1 : 0).sum() * 25;
        long hoursUntilExpiry = Math.max(1, ChronoUnit.HOURS.between(now, coupon.getExpiryDate()));
        int urgencyScore = hoursUntilExpiry <= 72 ? (int) (72 - hoursUntilExpiry) : 0;
        return termScore + score(worked, notWorked) + urgencyScore;
    }

    private boolean matchesAnyTerm(Coupon coupon, List<String> terms) {
        String searchable = searchableText(coupon);
        return terms.stream().anyMatch(searchable::contains);
    }

    private String searchableText(Coupon coupon) {
        return ((coupon.getName() == null ? "" : coupon.getName()) + " "
                + (coupon.getDescription() == null ? "" : coupon.getDescription()) + " "
                + (coupon.getCouponCode() == null ? "" : coupon.getCouponCode()))
                .toLowerCase(Locale.ROOT);
    }

    private List<String> queryTerms(String query) {
        return Pattern.compile("[a-zA-Z0-9]+")
                .matcher(query.toLowerCase(Locale.ROOT))
                .results()
                .map(MatchResultAdapter::group)
                .filter(term -> term.length() > 2)
                .filter(term -> !ASSISTANT_STOP_WORDS.contains(term))
                .distinct()
                .toList();
    }

    private Integer extractMaxDays(String query) {
        String clean = query.toLowerCase(Locale.ROOT);
        if (clean.contains("today")) return 0;
        if (clean.contains("tomorrow")) return 1;

        Matcher matcher = Pattern.compile("(?:under|within|next|in)?\\s*(\\d+)\\s+day").matcher(clean);
        if (matcher.find()) {
            return Math.max(0, Math.min(Integer.parseInt(matcher.group(1)), 30));
        }
        return null;
    }

    private String reason(Coupon coupon, List<String> terms, long days, int score) {
        boolean textMatch = terms.isEmpty() || matchesAnyTerm(coupon, terms);
        return (textMatch ? "Matched your search" : "Closest active match")
                + ", expires in " + days + " day(s)"
                + ", " + score + "% worked score";
    }

    private String riskLevel(Coupon coupon, long worked, long notWorked) {
        if (notWorked >= 2 && notWorked > worked) return "HIGH";
        if (coupon.isExpired()) return "EXPIRED";
        if (worked + notWorked == 0) return "UNVERIFIED";
        return "NORMAL";
    }

    private long daysUntilExpiry(Coupon coupon) {
        if (coupon.getExpiryDate() == null) return 0;
        return ChronoUnit.DAYS.between(LocalDateTime.now().toLocalDate(), coupon.getExpiryDate().toLocalDate());
    }

    private int score(long worked, long notWorked) {
        long total = worked + notWorked;
        return total == 0 ? 0 : (int) Math.round((worked * 100.0) / total);
    }

    private Coupon getCoupon(Long couponId) {
        return couponRepository.findById(couponId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COUPON_NOT_FOUND, "Coupon not found"));
    }

    private static class MatchResultAdapter {
        static String group(java.util.regex.MatchResult result) {
            return result.group();
        }
    }
}
