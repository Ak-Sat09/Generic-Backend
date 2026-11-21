package com.buytogether.group;

import com.buytogether.common.error.BusinessException;
import com.buytogether.common.error.ErrorCode;
import com.buytogether.group.cache.GroupCache;
import com.buytogether.group.member.GroupMember;
import com.buytogether.group.member.GroupMemberRepository;
import com.buytogether.support.MutableClock;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GroupServiceTest {

    private static final String GROUP_ID = "g1";
    private static final String CREATOR = "creator";
    private static final String USER = "user";

    private final GroupRepository groups = mock(GroupRepository.class);
    private final GroupMemberRepository members = mock(GroupMemberRepository.class);
    private final GroupCache cache = mock(GroupCache.class);
    private final MutableClock clock = new MutableClock(Instant.parse("2026-11-21T10:00:00Z"), ZoneOffset.UTC);
    private final GroupProperties properties = new GroupProperties(2, 50, 3, 90, 30, "0 5 0 * * *");

    private GroupService groupService;

    @BeforeEach
    void setUp() {
        groupService = new GroupService(groups, members, properties, cache, clock);
    }

    @Test
    void joiningTakesASeatAndEvictsTheCache() {
        givenGroup(group(GroupStatus.OPEN, 3, 10));
        when(groups.takeSeat(eq(GROUP_ID), eq(10), any())).thenReturn(Optional.of(group(GroupStatus.OPEN, 4, 10)));

        groupService.join(GROUP_ID, USER);

        verify(members).insert(any(GroupMember.class));
        verify(groups, never()).markFullIfTargetReached(anyString(), anyInt(), any());
        verify(cache).evict(GROUP_ID);
    }

    @Test
    void lastSeatMarksTheGroupFull() {
        givenGroup(group(GroupStatus.OPEN, 9, 10));
        when(groups.takeSeat(eq(GROUP_ID), eq(10), any())).thenReturn(Optional.of(group(GroupStatus.OPEN, 10, 10)));

        groupService.join(GROUP_ID, USER);

        verify(groups).markFullIfTargetReached(eq(GROUP_ID), eq(10), any());
    }

    @Test
    void losingTheRaceForTheLastSeatUndoesTheMembership() {
        givenGroup(group(GroupStatus.OPEN, 9, 10));
        when(groups.takeSeat(eq(GROUP_ID), eq(10), any())).thenReturn(Optional.empty());

        assertError(() -> groupService.join(GROUP_ID, USER), ErrorCode.GROUP_FULL);
        verify(members).deleteByGroupIdAndUserId(GROUP_ID, USER);
    }

    @Test
    void cannotJoinTwice() {
        givenGroup(group(GroupStatus.OPEN, 3, 10));
        when(members.insert(any(GroupMember.class))).thenThrow(new DuplicateKeyException("duplicate"));

        assertError(() -> groupService.join(GROUP_ID, USER), ErrorCode.ALREADY_MEMBER);
        verify(groups, never()).takeSeat(anyString(), anyInt(), any());
    }

    @Test
    void cannotJoinFullOrClosedGroups() {
        givenGroup(group(GroupStatus.FULL, 10, 10));
        assertError(() -> groupService.join(GROUP_ID, USER), ErrorCode.GROUP_FULL);

        givenGroup(group(GroupStatus.CLOSED, 4, 10));
        assertError(() -> groupService.join(GROUP_ID, USER), ErrorCode.GROUP_NOT_JOINABLE);
    }

    @Test
    void creatorCannotLeave() {
        givenGroup(group(GroupStatus.OPEN, 3, 10));

        assertError(() -> groupService.leave(GROUP_ID, CREATOR), ErrorCode.CREATOR_CANNOT_LEAVE);
    }

    @Test
    void leavingWithoutMembershipFails() {
        givenGroup(group(GroupStatus.OPEN, 3, 10));
        when(members.deleteByGroupIdAndUserId(GROUP_ID, USER)).thenReturn(0L);

        assertError(() -> groupService.leave(GROUP_ID, USER), ErrorCode.NOT_A_MEMBER);
        verify(groups, never()).releaseSeat(anyString(), any());
    }

    @Test
    void cannotCreateWhileAnOpenGroupExistsForSameBrandAndCity() {
        when(groups.existsByBrandAndCityKeyAndStatus(CarBrand.MAHINDRA, "jaipur", GroupStatus.OPEN)).thenReturn(true);
        CreateGroupRequest request = new CreateGroupRequest(CarBrand.MAHINDRA, "Thar", " JAIPUR ", 10, null, null);

        assertError(() -> groupService.create(CREATOR, request), ErrorCode.OPEN_GROUP_EXISTS);
        verify(groups, never()).save(any(BuyingGroup.class));
    }

    @Test
    void creatorCanMarkDealDone() {
        givenGroup(group(GroupStatus.FULL, 10, 10));
        when(groups.finish(eq(GROUP_ID), eq(GroupStatus.COMPLETED), any())).thenReturn(true);

        groupService.complete(GROUP_ID, CREATOR);

        verify(groups).finish(eq(GROUP_ID), eq(GroupStatus.COMPLETED), any());
        verify(cache).evict(GROUP_ID);
    }

    @Test
    void memberCannotMarkDealDone() {
        givenGroup(group(GroupStatus.FULL, 10, 10));

        assertError(() -> groupService.complete(GROUP_ID, USER), ErrorCode.NOT_GROUP_CREATOR);
        verify(groups, never()).finish(anyString(), any(), any());
    }

    @Test
    void cannotCompleteAFinishedGroup() {
        givenGroup(group(GroupStatus.CLOSED, 4, 10));
        when(groups.finish(eq(GROUP_ID), eq(GroupStatus.COMPLETED), any())).thenReturn(false);

        assertError(() -> groupService.complete(GROUP_ID, CREATOR), ErrorCode.GROUP_INACTIVE);
    }

    @Test
    void onlyCreatorCanClose() {
        givenGroup(group(GroupStatus.OPEN, 3, 10));

        assertError(() -> groupService.close(GROUP_ID, USER), ErrorCode.NOT_GROUP_CREATOR);
    }

    @Test
    void rejectsSizeOutsideConfiguredRange() {
        CreateGroupRequest tooBig = new CreateGroupRequest(CarBrand.TATA, "Nexon", "Jaipur", 500, null, null);

        assertError(() -> groupService.create(CREATOR, tooBig), ErrorCode.INVALID_GROUP_SIZE);
    }

    @Test
    void rejectsDeadlineTooFarAhead() {
        LocalDate farAway = LocalDate.of(2027, 6, 1);
        CreateGroupRequest request = new CreateGroupRequest(CarBrand.TATA, "Nexon", "Jaipur", 10, null, farAway);

        assertError(() -> groupService.create(CREATOR, request), ErrorCode.INVALID_DEADLINE);
    }

    @Test
    void limitsOpenGroupsPerUser() {
        when(groups.countByCreatorIdAndStatusIn(CREATOR, GroupStatus.ACTIVE)).thenReturn(3L);
        CreateGroupRequest request = new CreateGroupRequest(CarBrand.TATA, "Nexon", "Jaipur", 10, null, null);

        assertError(() -> groupService.create(CREATOR, request), ErrorCode.TOO_MANY_ACTIVE_GROUPS);
    }

    @Test
    void createStoresCleanNamesAndAddsCreatorAsMember() {
        when(groups.save(any(BuyingGroup.class))).thenAnswer(invocation -> invocation.getArgument(0));
        CreateGroupRequest request = new CreateGroupRequest(CarBrand.TATA, "Nexon   EV", " Jaipur", 10, "Diwali offer", null);

        GroupView view = groupService.create(CREATOR, request);

        assertThat(view.brand()).isEqualTo(CarBrand.TATA);
        assertThat(view.city()).isEqualTo("Jaipur");
        assertThat(view.model()).isEqualTo("Nexon EV");
        assertThat(view.memberCount()).isEqualTo(1);
        assertThat(view.status()).isEqualTo(GroupStatus.OPEN);
        verify(members).insert(any(GroupMember.class));
    }

    private void givenGroup(BuyingGroup group) {
        when(groups.findById(GROUP_ID)).thenReturn(Optional.of(group));
    }

    private static BuyingGroup group(GroupStatus status, int memberCount, int targetSize) {
        return BuyingGroup.builder()
                .id(GROUP_ID)
                .brand(CarBrand.TATA).model("Nexon").city("Jaipur")
                .modelKey("nexon").cityKey("jaipur")
                .targetSize(targetSize)
                .memberCount(memberCount)
                .status(status)
                .creatorId(CREATOR)
                .createdAt(Instant.parse("2026-11-20T10:00:00Z"))
                .build();
    }

    private static void assertError(ThrowingCallable call, ErrorCode expected) {
        assertThatThrownBy(call).isInstanceOfSatisfying(BusinessException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(expected));
    }
}
