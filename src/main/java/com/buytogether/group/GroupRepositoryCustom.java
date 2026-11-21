package com.buytogether.group;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

/**
 * Operations that need atomic, conditional updates in MongoDB.
 */
public interface GroupRepositoryCustom {

    /** Adds one member if the group is open and below its target. Empty when no seat was taken. */
    Optional<BuyingGroup> takeSeat(String groupId, int targetSize, Instant now);

    /** Gives a seat back and reopens the group if it was full. Empty when the group is not active. */
    Optional<BuyingGroup> releaseSeat(String groupId, Instant now);

    boolean markFullIfTargetReached(String groupId, int targetSize, Instant now);

    /** Moves an active group to a finished state (completed or closed). False if it was not active. */
    boolean finish(String groupId, GroupStatus finalStatus, Instant now);

    long expireDeadlinesBefore(LocalDate day, Instant now);

    Page<BuyingGroup> search(GroupSearch search, Pageable pageable);
}
