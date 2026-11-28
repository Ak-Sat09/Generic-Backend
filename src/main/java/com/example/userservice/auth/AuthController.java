package com.example.userservice.auth;

import com.example.userservice.common.ApiResponse;
import com.example.userservice.token.LegacyTokenService;
import com.example.userservice.user.User;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The original endpoints, unchanged for the blog and coupon frontends. Login now goes through the same
 * lockout as the new login, and still returns the old 24 hour token as {@code data}.
 */
@RestController
@RequestMapping("/api/users")
public class AuthController {

    private final UserAuthService auth;
    private final LegacyTokenService legacyTokens;

    public AuthController(UserAuthService auth, LegacyTokenService legacyTokens) {
        this.auth = auth;
        this.legacyTokens = legacyTokens;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse> register(@Valid @RequestBody RegisterRequest request) {
        User user = auth.register(request.name(), request.email(), request.password());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Registration successful", user.getId()));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse> login(@Valid @RequestBody LoginRequest request) {
        User user = auth.authenticate(request.email(), request.password());
        return ResponseEntity.ok(ApiResponse.ok("Login successful", legacyTokens.issue(user)));
    }

    public record RegisterRequest(@NotBlank @Size(min = 2, max = 120) String name,
                                  @NotBlank @Email @Size(max = 254) String email,
                                  @NotBlank @Size(min = 8, max = 72) String password) {
    }

    public record LoginRequest(@NotBlank @Email @Size(max = 254) String email,
                               @NotBlank @Size(max = 128) String password) {
    }
}
