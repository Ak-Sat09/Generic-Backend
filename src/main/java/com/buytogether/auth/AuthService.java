package com.buytogether.auth;

import com.buytogether.auth.google.GoogleIdentity;
import com.buytogether.auth.google.GoogleTokenVerifier;
import com.buytogether.auth.otp.OtpService;
import com.buytogether.auth.otp.OtpService.OtpChallenge;
import com.buytogether.common.email.EmailAddress;
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

/**
 * Three ways in (phone OTP, email OTP, Google), one way out: our own JWT.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final OtpService otpService;
    private final GoogleTokenVerifier googleVerifier;
    private final UserService userService;
    private final JwtService jwtService;

    public OtpChallenge sendOtp(String rawPhone) {
        return otpService.send(PhoneNumber.parse(rawPhone));
    }

    public LoginResponse login(String rawPhone, String otp, String name) {
        PhoneNumber phone = PhoneNumber.parse(rawPhone);
        otpService.verify(phone, otp);
        return issueToken(userService.signIn(phone, name));
    }

    public OtpChallenge sendEmailOtp(String rawEmail) {
        return otpService.send(EmailAddress.parse(rawEmail));
    }

    public LoginResponse loginWithEmail(String rawEmail, String otp, String name) {
        EmailAddress email = EmailAddress.parse(rawEmail);
        otpService.verify(email, otp);
        return issueToken(userService.signIn(email, name));
    }

    public LoginResponse loginWithGoogle(String idToken) {
        GoogleIdentity google = googleVerifier.verify(idToken);
        return issueToken(userService.signIn(google.email(), google.name()));
    }

    private LoginResponse issueToken(SignInResult signIn) {
        User user = signIn.user();
        IssuedToken token = jwtService.issue(new AuthUser(user.getId()));
        return new LoginResponse(token.token(), token.expiresAt(), signIn.newUser(), UserResponse.from(user));
    }
}
