package com.buytogether.auth;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class GoogleLoginController {

    private final AuthService authService;

    @PostMapping("/api/auth/google")
    public LoginResponse login(@Valid @RequestBody GoogleLoginRequest request) {
        return authService.loginWithGoogle(request.credential());
    }
}
