package com.buytogether.group;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Immutable snapshot of a group that is the same for every viewer, which makes it safe to cache.
 */
public record GroupView(
        String id,
        CarBrand brand,
        String model,
        String city,
        String note,
        int targetSize,
        int memberCount,
        int spotsLeft,
        GroupStatus status,
        String creatorId,
        LocalDate deadline,
        Instant createdAt
) {

    public static GroupView from(BuyingGroup group) {
        return new GroupView(
                group.getId(),
                group.getBrand(),
                group.getModel(),
                group.getCity(),
                group.getNote(),
                group.getTargetSize(),
                group.getMemberCount(),
                group.spotsLeft(),
                group.getStatus(),
                group.getCreatorId(),
                group.getDeadline(),
                group.getCreatedAt()
        );
    }

    public boolean isCreatedBy(String userId) {
        return creatorId.equals(userId);
    }
}
