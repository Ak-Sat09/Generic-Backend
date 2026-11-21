package com.buytogether.group;

import com.buytogether.group.cache.GroupCache;
import com.buytogether.group.member.GroupMemberRepository;
import com.buytogether.support.MutableClock;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class GroupPurgeServiceTest {

    private final GroupRepository groups = mock(GroupRepository.class);
    private final GroupMemberRepository members = mock(GroupMemberRepository.class);
    private final GroupCache cache = mock(GroupCache.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final MutableClock clock = new MutableClock(Instant.parse("2026-11-21T10:00:00Z"), ZoneOffset.UTC);

    private final GroupPurgeService purgeService = new GroupPurgeService(groups, members,
            new GroupProperties(2, 50, 3, 90, 30, "0 5 0 * * *"), cache, events, clock);

    @Test
    void deletesGroupsFinishedLongerThanRetentionTogetherWithTheirData() {
        when(groups.findByStatusInAndUpdatedAtBefore(eq(GroupStatus.FINISHED),
                eq(Instant.parse("2026-10-22T10:00:00Z")), any(Pageable.class)))
                .thenReturn(List.of(group("g1"), group("g2")));

        int purged = purgeService.purgeFinishedGroups();

        assertThat(purged).isEqualTo(2);
        InOrder order = inOrder(events, members, groups);
        order.verify(events).publishEvent(new GroupsPurgedEvent(List.of("g1", "g2")));
        order.verify(members).deleteByGroupIdIn(List.of("g1", "g2"));
        order.verify(groups).deleteAllById(List.of("g1", "g2"));
        verify(cache).evictAll();
    }

    @Test
    void doesNothingWhenThereIsNothingToPurge() {
        when(groups.findByStatusInAndUpdatedAtBefore(any(), any(), any(Pageable.class))).thenReturn(List.of());

        assertThat(purgeService.purgeFinishedGroups()).isZero();
        verifyNoInteractions(events, members, cache);
        verify(groups, never()).deleteAllById(any());
    }

    private static BuyingGroup group(String id) {
        return BuyingGroup.builder().id(id).status(GroupStatus.COMPLETED).creatorId("creator").build();
    }
}
