package com.buytogether.group;

import com.buytogether.group.cache.GroupCache;
import com.buytogether.group.member.GroupMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Deletes groups that finished (completed, closed or expired) more than
 * {@code buy-together.groups.purge-after-days} ago, together with everything attached to them.
 */
@Service
@RequiredArgsConstructor
public class GroupPurgeService {

    private static final int BATCH_SIZE = 200;

    private final GroupRepository groups;
    private final GroupMemberRepository members;
    private final GroupProperties properties;
    private final GroupCache cache;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public int purgeFinishedGroups() {
        Instant cutoff = clock.instant().minus(Duration.ofDays(properties.purgeAfterDays()));
        int purged = 0;

        List<BuyingGroup> batch;
        do {
            batch = groups.findByStatusInAndUpdatedAtBefore(GroupStatus.FINISHED, cutoff, PageRequest.of(0, BATCH_SIZE));
            if (batch.isEmpty()) {
                break;
            }
            List<String> ids = batch.stream().map(BuyingGroup::getId).toList();

            // Children first: if anything fails half way, the groups are still there and get retried next run
            events.publishEvent(new GroupsPurgedEvent(ids));
            members.deleteByGroupIdIn(ids);
            groups.deleteAllById(ids);
            purged += ids.size();
        } while (batch.size() == BATCH_SIZE);

        if (purged > 0) {
            cache.evictAll();
        }
        return purged;
    }
}
