package com.buytogether.user;

import com.buytogether.security.AuthUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/me")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public UserResponse me(@AuthenticationPrincipal AuthUser authUser) {
        return UserResponse.from(userService.get(authUser.id()));
    }

    @PutMapping
    public UserResponse updateProfile(@AuthenticationPrincipal AuthUser authUser,
                                      @Valid @RequestBody UpdateProfileRequest request) {
        return UserResponse.from(userService.updateProfile(authUser.id(), request.name(), request.city()));
    }
}
