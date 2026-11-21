package com.buytogether.auth;

import com.buytogether.auth.otp.OtpService.OtpChallenge;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/otp")
@RequiredArgsConstructor
public class OtpLoginController {

    private final AuthService authService;

    @PostMapping("/send")
    public OtpChallenge sendOtp(@Valid @RequestBody SendOtpRequest request) {
        return authService.sendOtp(request.phone());
    }

    @PostMapping("/verify")
    public LoginResponse verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        return authService.login(request.phone(), request.otp(), request.name());
    }
}
