package com.example.userservice.auth;

import com.example.userservice.account.AccountLinkService;
import com.example.userservice.auth.TokenAuthController.TokenResponse;
import com.example.userservice.auth.TokenAuthController.UserView;
import com.example.userservice.common.ApiResponse;
import com.example.userservice.common.UserServiceException;
import com.example.userservice.google.GoogleAccountVerifier;
import com.example.userservice.google.GoogleAccountVerifier.GoogleIdentity;
import com.example.userservice.otp.AccountOtpService;
import com.example.userservice.otp.AccountOtpService.Destination;
import com.example.userservice.token.AccessTokenVerifier;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Email links (confirm email, forgot / reset password), one-time codes by SMS or email, and Google.
 */
@RestController
@RequestMapping("/api/users/auth")
public class AccountController {

    private final UserAuthService auth;
    private final AccountLinkService links;
    private final AccountOtpService otp;
    private final GoogleAccountVerifier google;
    private final AccessTokenVerifier verifier;

    public AccountController(UserAuthService auth, AccountLinkService links, AccountOtpService otp,
                             GoogleAccountVerifier google, AccessTokenVerifier verifier) {
        this.auth = auth;
        this.links = links;
        this.otp = otp;
        this.google = google;
        this.verifier = verifier;
    }

    /** Which login options the frontend should show. */
    @GetMapping("/options")
    public ResponseEntity<ApiResponse> options() {
        return ResponseEntity.ok(ApiResponse.ok("Login options", Map.of("google", google.enabled(), "otp", true)));
    }

    @PostMapping("/verify-email/send")
    public ResponseEntity<ApiResponse> sendVerification(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
                                                        @Valid @RequestBody(required = false) AppUrlRequest request) {
        Long userId = verifier.userId(authorization).orElseThrow(() -> UserServiceException.unauthorized("Please log in again"));
        links.sendVerification(auth.get(userId), request == null ? null : request.appUrl());
        return ResponseEntity.ok(ApiResponse.ok("If your email isn't confirmed yet, we sent you a link", null));
    }

    @PostMapping("/verify-email/confirm")
    public ResponseEntity<ApiResponse> confirmEmail(@Valid @RequestBody TokenRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Email confirmed", UserView.from(links.confirmEmail(request.token()))));
    }

    @PostMapping("/password/forgot")
    public ResponseEntity<ApiResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        links.forgotPassword(request.email(), request.appUrl());
        return ResponseEntity.ok(ApiResponse.ok("If an account uses this email, we sent a link to reset the password", null));
    }

    @PostMapping("/password/reset")
    public ResponseEntity<ApiResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        links.resetPassword(request.token(), request.password());
        return ResponseEntity.ok(ApiResponse.ok("Password changed. Log in with your new password.", null));
    }

    @PostMapping("/otp/send")
    public ResponseEntity<ApiResponse> sendCode(@Valid @RequestBody OtpSendRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Code sent", otp.send(Destination.of(request.channel(), request.destination()))));
    }

    @PostMapping("/otp/verify")
    public ResponseEntity<ApiResponse> verifyCode(@Valid @RequestBody OtpVerifyRequest request) {
        Destination to = Destination.of(request.channel(), request.destination());
        otp.verify(to, request.code());
        UserAuthService.TokenPair pair = to.channel() == AccountOtpService.Channel.PHONE
                ? auth.loginWithVerifiedPhone(to.value(), request.name())
                : auth.loginWithVerifiedEmail(to.value(), request.name());
        return ResponseEntity.ok(ApiResponse.ok("Login successful", TokenResponse.from(pair)));
    }

    @PostMapping("/google")
    public ResponseEntity<ApiResponse> google(@Valid @RequestBody GoogleRequest request) {
        GoogleIdentity identity = google.verify(request.credential());
        return ResponseEntity.ok(ApiResponse.ok("Login successful", TokenResponse.from(auth.loginWithVerifiedEmail(identity.email(), identity.name()))));
    }

    public record AppUrlRequest(@Size(max = 200) String appUrl) {
    }

    public record TokenRequest(@NotBlank @Size(max = 200) String token) {
    }

    public record ForgotPasswordRequest(@NotBlank @Email @Size(max = 254) String email, @Size(max = 200) String appUrl) {
    }

    public record ResetPasswordRequest(@NotBlank @Size(max = 200) String token, @NotBlank @Size(min = 8, max = 72) String password) {
    }

    public record OtpSendRequest(@NotBlank @Pattern(regexp = "phone|email") String channel, @NotBlank @Size(max = 254) String destination) {
    }

    public record OtpVerifyRequest(@NotBlank @Pattern(regexp = "phone|email") String channel, @NotBlank @Size(max = 254) String destination,
                                   @NotBlank @Pattern(regexp = "\\d{4,8}", message = "must be digits only") String code,
                                   @Size(max = 120) String name) {
    }

    public record GoogleRequest(@NotBlank @Size(max = 4096) String credential) {
    }
}
