package com.buytogether.group;

import com.buytogether.common.error.BusinessException;
import com.buytogether.common.error.ErrorCode;
import com.buytogether.common.text.Text;
import com.buytogether.common.web.PageResponse;
import com.buytogether.common.web.Paging;
import com.buytogether.group.cache.GroupCache;
import com.buytogether.group.cache.GroupCache.SearchKey;
import com.buytogether.group.member.GroupMember;
import com.buytogether.group.member.GroupMemberRepository;
import com.buytogether.group.member.MemberResponse;
import com.buytogether.user.User;
import com.buytogether.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GroupQueryService {

    private static final Sort NEWEST_FIRST = Sort.by(Sort.Direction.DESC, "createdAt");
    private static final Sort RECENTLY_JOINED = Sort.by(Sort.Direction.DESC, "joinedAt");
    private static final Sort FIRST_JOINED = Sort.by(Sort.Direction.ASC, "joinedAt");
    private static final int MAX_SUGGESTIONS = 10;

    private final GroupRepository groups;
    private final GroupMemberRepository members;
    private final UserService userService;
    private final GroupCache cache;

    public GroupView view(String groupId) {
        return cache.group(groupId, () -> groups.findById(groupId)
                .map(GroupView::from)
                .orElseThrow(() -> new BusinessException(ErrorCode.GROUP_NOT_FOUND)));
    }

    /** @param viewerId logged in user, or {@code null} for anonymous visitors */
    public GroupResponse get(String groupId, String viewerId) {
        GroupView view = view(groupId);
        boolean joined = viewerId != null && members.existsByGroupIdAndUserId(groupId, viewerId);
        return GroupResponse.of(view, joined, view.isCreatedBy(viewerId));
    }

    public PageResponse<GroupResponse> browse(GroupSearch search, int page, int size, String viewerId) {
        Pageable pageable = Paging.of(page, size, NEWEST_FIRST);
        SearchKey key = new SearchKey(search, pageable.getPageNumber(), pageable.getPageSize());
        PageResponse<GroupView> views = cache.search(key,
                () -> PageResponse.from(groups.search(search, pageable), GroupView::from));

        Set<String> joined = joinedGroupIds(viewerId, views.content());
        return views.map(view -> GroupResponse.of(view, joined.contains(view.id()), view.isCreatedBy(viewerId)));
    }

    /**
     * Open groups for the same brand in the same city, biggest first. Read straight from
     * the database (not the cache) because it decides whether a new group may be created.
     */
    public GroupSuggestions suggestions(CarBrand brand, String city, String viewerId) {
        List<GroupView> open = groups.findByBrandAndCityKeyAndStatusOrderByMemberCountDescCreatedAtAsc(
                        brand, Text.key(city), GroupStatus.OPEN, PageRequest.of(0, MAX_SUGGESTIONS))
                .stream()
                .map(GroupView::from)
                .toList();

        Set<String> joined = joinedGroupIds(viewerId, open);
        List<GroupResponse> responses = open.stream()
                .map(view -> GroupResponse.of(view, joined.contains(view.id()), view.isCreatedBy(viewerId)))
                .toList();
        int peopleInterested = open.stream().mapToInt(GroupView::memberCount).sum();

        return new GroupSuggestions(brand, brand.displayName(), Text.clean(city), open.isEmpty(), peopleInterested, responses);
    }

    public PageResponse<GroupResponse> joinedBy(String userId, int page, int size) {
        Page<GroupMember> memberships = members.findByUserId(userId, Paging.of(page, size, RECENTLY_JOINED));
        return PageResponse.from(memberships, membership -> {
            GroupView view = view(membership.getGroupId());
            return GroupResponse.of(view, true, view.isCreatedBy(userId));
        });
    }

    public PageResponse<MemberResponse> members(String groupId, String viewerId, int page, int size) {
        requireMember(groupId, viewerId);
        Page<GroupMember> memberships = members.findByGroupId(groupId, Paging.of(page, size, FIRST_JOINED));
        List<String> userIds = memberships.map(GroupMember::getUserId).toList();
        Map<String, User> users = userService.findAllById(userIds);
        return PageResponse.from(memberships, membership -> MemberResponse.of(membership, users.get(membership.getUserId())));
    }

    public GroupView requireMember(String groupId, String userId) {
        GroupView view = view(groupId);
        if (!members.existsByGroupIdAndUserId(groupId, userId)) {
            throw new BusinessException(ErrorCode.NOT_A_MEMBER);
        }
        return view;
    }

    public GroupView requireActiveMember(String groupId, String userId) {
        GroupView view = requireMember(groupId, userId);
        if (!view.status().isActive()) {
            throw new BusinessException(ErrorCode.GROUP_INACTIVE);
        }
        return view;
    }

    private Set<String> joinedGroupIds(String viewerId, List<GroupView> views) {
        if (viewerId == null || views.isEmpty()) {
            return Set.of();
        }
        List<String> ids = views.stream().map(GroupView::id).toList();
        return members.findByUserIdAndGroupIdIn(viewerId, ids).stream()
                .map(GroupMember::getGroupId)
                .collect(Collectors.toSet());
    }
}
