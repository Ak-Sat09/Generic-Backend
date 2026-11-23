package com.buytogether.auth;

import com.buytogether.auth.otp.OtpService.OtpChallenge;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/email")
@RequiredArgsConstructor
public class EmailLoginController {

    private final AuthService authService;

    @PostMapping("/send")
    public OtpChallenge sendOtp(@Valid @RequestBody SendEmailOtpRequest request) {
        return authService.sendEmailOtp(request.email());
    }

    @PostMapping("/verify")
    public LoginResponse verifyOtp(@Valid @RequestBody VerifyEmailOtpRequest request) {
        return authService.loginWithEmail(request.email(), request.otp(), request.name());
    }
}
