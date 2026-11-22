package com.buytogether.group;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.time.LocalDate;

/**
 * A "who wants to buy this car with me" post. Membership counters and status are only
 * changed through atomic updates in {@link GroupRepositoryCustom}, never by saving this
 * object, so concurrent joins cannot overwrite each other.
 */
@Document("bt_groups")
@CompoundIndexes({
        @CompoundIndex(name = "browse_idx", def = "{'status': 1, 'cityKey': 1, 'brand': 1, 'createdAt': -1}"),
        @CompoundIndex(name = "creator_idx", def = "{'creatorId': 1, 'status': 1}"),
        @CompoundIndex(name = "expiry_idx", def = "{'status': 1, 'deadline': 1}"),
        @CompoundIndex(name = "purge_idx", def = "{'status': 1, 'updatedAt': 1}")
})
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class BuyingGroup {

    @Id
    private String id;

    private CarBrand brand;
    private String model;
    private String city;

    // Lower case copies used for case insensitive filtering
    private String modelKey;
    private String cityKey;

    private String note;

    private int targetSize;
    private int memberCount;
    private GroupStatus status;

    private String creatorId;
    private LocalDate deadline;

    private Instant createdAt;
    private Instant updatedAt;

    public boolean isCreatedBy(String userId) {
        return creatorId.equals(userId);
    }

    public int spotsLeft() {
        return Math.max(0, targetSize - memberCount);
    }
}
