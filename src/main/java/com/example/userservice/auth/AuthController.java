package com.example.userservice.auth;

import com.example.userservice.user.User;
import com.example.userservice.user.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

@RestController
@RequestMapping("/api/users")
public class AuthController {
    private final UserRepository users;
    private final PasswordEncoder passwords;
    private final Key jwtKey;
    private final long expirationMs;

    public AuthController(UserRepository users, PasswordEncoder passwords,
                          @Value("${app.jwt.secret}") String secret,
                          @Value("${app.jwt.expiration-ms:86400000}") long expirationMs) {
        if (secret.length() < 32) throw new IllegalStateException("JWT secret must be at least 32 characters");
        this.users = users; this.passwords = passwords;
        this.jwtKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse> register(@Valid @RequestBody RegisterRequest request) {
        String email = normalize(request.email());
        if (users.existsByEmail(email)) return response(HttpStatus.CONFLICT, false, "Email is already registered", null);
        User user = users.save(new User(request.name().trim(), email, passwords.encode(request.password())));
        return response(HttpStatus.CREATED, true, "Registration successful", user.getId());
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse> login(@Valid @RequestBody LoginRequest request) {
        User user = users.findByEmail(normalize(request.email())).orElse(null);
        if (user == null || !passwords.matches(request.password(), user.getPasswordHash()))
            return response(HttpStatus.UNAUTHORIZED, false, "Invalid email or password", null);
        return response(HttpStatus.OK, true, "Login successful", tokenFor(user));
    }

    private String tokenFor(User user) {
        Date now = new Date();
        return Jwts.builder().setSubject(user.getEmail()).claim("userId", user.getId())
                .setIssuedAt(now).setExpiration(new Date(now.getTime() + expirationMs))
                .signWith(jwtKey, SignatureAlgorithm.HS256).compact();
    }

    private static String normalize(String email) { return email.trim().toLowerCase(); }
    private static ResponseEntity<ApiResponse> response(HttpStatus status, boolean success, String message, Object data) {
        return ResponseEntity.status(status).body(new ApiResponse(success, message, data));
    }

    public record RegisterRequest(@NotBlank @Size(min=2, max=120) String name, @NotBlank @Email String email,
                                  @NotBlank @Size(min=8, max=128) String password) {}
    public record LoginRequest(@NotBlank @Email String email, @NotBlank @Size(min=8, max=128) String password) {}
    public record ApiResponse(boolean success, String message, Object data) {}
}
