package com.example.userservice.auth;

import com.example.userservice.auth.AuthController.LoginRequest;
import com.example.userservice.auth.AuthService.TokenPair;
import com.example.userservice.common.ApiResponse;
import com.example.userservice.common.UserServiceException;
import com.example.userservice.token.AccessTokenVerifier;
import com.example.userservice.user.User;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

/**
 * The new login: a 15 minute access token plus a refresh token, with refresh and logout.
 */
@RestController
@RequestMapping("/api/users")
public class TokenAuthController {

    private final AuthService auth;
    private final AccessTokenVerifier verifier;

    public TokenAuthController(AuthService auth, AccessTokenVerifier verifier) {
        this.auth = auth;
        this.verifier = verifier;
    }

    @PostMapping("/auth/login")
    public ResponseEntity<ApiResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Login successful", TokenResponse.from(auth.login(request.email(), request.password()))));
    }

    @PostMapping("/auth/refresh")
    public ResponseEntity<ApiResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Token refreshed", TokenResponse.from(auth.refresh(request.refreshToken()))));
    }

    @PostMapping("/auth/logout")
    public ResponseEntity<ApiResponse> logout(@Valid @RequestBody RefreshRequest request) {
        auth.logout(request.refreshToken());
        return ResponseEntity.ok(ApiResponse.ok("Logged out", null));
    }

    @PostMapping("/auth/logout-all")
    public ResponseEntity<ApiResponse> logoutEverywhere(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        auth.logoutEverywhere(requireUserId(authorization));
        return ResponseEntity.ok(ApiResponse.ok("Logged out on all devices", null));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse> me(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        return ResponseEntity.ok(ApiResponse.ok("Current user", UserView.from(auth.get(requireUserId(authorization)))));
    }

    private Long requireUserId(String authorization) {
        return verifier.userId(authorization).orElseThrow(() -> UserServiceException.unauthorized("Please log in again"));
    }

    public record RefreshRequest(@NotBlank @Size(max = 200) String refreshToken) {
    }

    public record UserView(Long id, String name, String email, boolean emailVerified, Instant createdAt) {
        static UserView from(User user) {
            return new UserView(user.getId(), user.getName(), user.getEmail(), user.isEmailVerified(), user.getCreatedAt());
        }
    }

    public record TokenResponse(String tokenType, String accessToken, long accessTokenExpiresIn,
                                String refreshToken, Instant refreshTokenExpiresAt, UserView user) {
        static TokenResponse from(TokenPair pair) {
            return new TokenResponse("Bearer", pair.accessToken().token(), pair.accessToken().expiresInSeconds(),
                    pair.refreshToken().token(), pair.refreshToken().expiresAt(), UserView.from(pair.user()));
        }
    }
}
