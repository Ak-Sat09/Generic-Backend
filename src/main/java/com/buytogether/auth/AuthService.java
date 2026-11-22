package com.buytogether.auth;

import com.buytogether.auth.otp.OtpService;
import com.buytogether.auth.otp.OtpService.OtpChallenge;
import com.buytogether.common.phone.PhoneNumber;
import com.buytogether.security.AuthUser;
import com.buytogether.security.JwtService;
import com.buytogether.security.JwtService.IssuedToken;
import com.buytogether.user.User;
import com.buytogether.user.UserResponse;
import com.buytogether.user.UserService;
import com.buytogether.user.UserService.SignInResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final OtpService otpService;
    private final UserService userService;
    private final JwtService jwtService;

    public OtpChallenge sendOtp(String rawPhone) {
        return otpService.send(PhoneNumber.parse(rawPhone));
    }

    public LoginResponse login(String rawPhone, String otp, String name) {
        PhoneNumber phone = PhoneNumber.parse(rawPhone);
        otpService.verify(phone, otp);

        SignInResult signIn = userService.signIn(phone, name);
        User user = signIn.user();
        IssuedToken token = jwtService.issue(new AuthUser(user.getId(), user.getPhone()));
        return new LoginResponse(token.token(), token.expiresAt(), signIn.newUser(), UserResponse.from(user));
    }
}
