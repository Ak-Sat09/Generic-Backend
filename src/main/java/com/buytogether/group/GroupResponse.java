package com.buytogether.group;

import java.time.Instant;
import java.time.LocalDate;

public record GroupResponse(
        String id,
        CarBrand brand,
        String brandName,
        String model,
        String city,
        String note,
        int targetSize,
        int memberCount,
        int spotsLeft,
        GroupStatus status,
        LocalDate deadline,
        Instant createdAt,
        boolean joined,
        boolean createdByMe
) {

    public static GroupResponse of(GroupView view, boolean joined, boolean createdByMe) {
        return new GroupResponse(
                view.id(),
                view.brand(),
                view.brand().displayName(),
                view.model(),
                view.city(),
                view.note(),
                view.targetSize(),
                view.memberCount(),
                view.spotsLeft(),
                view.status(),
                view.deadline(),
                view.createdAt(),
                joined,
                createdByMe
        );
    }
}
