package com.buytogether.group;

import com.buytogether.common.web.PageResponse;
import com.buytogether.group.member.MemberResponse;
import com.buytogether.security.AuthUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/groups")
@RequiredArgsConstructor
public class GroupController {

    private final GroupService groupService;
    private final GroupQueryService groupQueries;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GroupResponse create(@AuthenticationPrincipal AuthUser user,
                                @Valid @RequestBody CreateGroupRequest request) {
        return GroupResponse.of(groupService.create(user.id(), request), true, true);
    }

    @GetMapping
    public PageResponse<GroupResponse> browse(@RequestParam(required = false) String city,
                                              @RequestParam(required = false) CarBrand brand,
                                              @RequestParam(required = false) String model,
                                              @RequestParam(required = false) GroupStatus status,
                                              @RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "20") int size,
                                              @AuthenticationPrincipal AuthUser viewer) {
        GroupSearch search = GroupSearch.of(city, brand, model, status);
        return groupQueries.browse(search, page, size, idOf(viewer));
    }

    @GetMapping("/suggestions")
    public GroupSuggestions suggestions(@RequestParam CarBrand brand,
                                        @RequestParam String city,
                                        @AuthenticationPrincipal AuthUser viewer) {
        return groupQueries.suggestions(brand, city, idOf(viewer));
    }

    @GetMapping("/mine")
    public PageResponse<GroupResponse> mine(@AuthenticationPrincipal AuthUser user,
                                            @RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "20") int size) {
        return groupQueries.joinedBy(user.id(), page, size);
    }

    @GetMapping("/{groupId}")
    public GroupResponse get(@PathVariable String groupId, @AuthenticationPrincipal AuthUser viewer) {
        return groupQueries.get(groupId, idOf(viewer));
    }

    @PostMapping("/{groupId}/join")
    public GroupResponse join(@PathVariable String groupId, @AuthenticationPrincipal AuthUser user) {
        GroupView view = groupService.join(groupId, user.id());
        return GroupResponse.of(view, true, false);
    }

    @PostMapping("/{groupId}/leave")
    public GroupResponse leave(@PathVariable String groupId, @AuthenticationPrincipal AuthUser user) {
        GroupView view = groupService.leave(groupId, user.id());
        return GroupResponse.of(view, false, false);
    }

    @PostMapping("/{groupId}/close")
    public GroupResponse close(@PathVariable String groupId, @AuthenticationPrincipal AuthUser user) {
        GroupView view = groupService.close(groupId, user.id());
        return GroupResponse.of(view, true, true);
    }

    @PostMapping("/{groupId}/complete")
    public GroupResponse complete(@PathVariable String groupId, @AuthenticationPrincipal AuthUser user) {
        GroupView view = groupService.complete(groupId, user.id());
        return GroupResponse.of(view, true, true);
    }

    @GetMapping("/{groupId}/members")
    public PageResponse<MemberResponse> members(@PathVariable String groupId,
                                                @AuthenticationPrincipal AuthUser user,
                                                @RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "50") int size) {
        return groupQueries.members(groupId, user.id(), page, size);
    }

    private static String idOf(AuthUser user) {
        return user == null ? null : user.id();
    }
}
