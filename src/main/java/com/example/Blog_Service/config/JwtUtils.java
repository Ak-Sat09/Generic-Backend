package com.example.Blog_Service.config;

import com.example.userservice.token.AccessTokenVerifier;
import org.springframework.stereotype.Component;

/**
 * Reads the user id from a token issued by the User Service. All token checks live in
 * {@link AccessTokenVerifier}, so every module accepts exactly the same tokens.
 */
@Component("blogJwtUtils")
public class JwtUtils {

    private final AccessTokenVerifier verifier;

    public JwtUtils(AccessTokenVerifier verifier) {
        this.verifier = verifier;
    }

    public boolean validateToken(String token) {
        return verifier.userId(token).isPresent();
    }

    public Long getUserIdFromToken(String token) {
        return verifier.userId(token).orElseThrow(() -> new IllegalStateException("Invalid or expired token"));
    }
}
