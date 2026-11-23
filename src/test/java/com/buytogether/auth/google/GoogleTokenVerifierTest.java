package com.buytogether.auth.google;

import com.buytogether.common.error.BusinessException;
import com.buytogether.common.error.ErrorCode;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import java.time.Instant;
import java.util.Date;
import java.util.function.UnaryOperator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Uses a local RSA key in place of Google's, so the real decoder and validators are exercised offline.
 */
class GoogleTokenVerifierTest {

    private static final String CLIENT_ID = "client-123.apps.googleusercontent.com";
    private static RSAKey googleKey;
    private static RSAKey otherKey;

    @BeforeAll
    static void keys() throws Exception {
        googleKey = new RSAKeyGenerator(2048).generate();
        otherKey = new RSAKeyGenerator(2048).generate();
    }

    @Test
    void acceptsAValidToken() {
        GoogleIdentity identity = verifier(CLIENT_ID).verify(token(googleKey, claims -> claims));

        assertThat(identity.email().value()).isEqualTo("anmol@gmail.com");
        assertThat(identity.name()).isEqualTo("Anmol Mehla");
        assertThat(identity.subject()).isEqualTo("google-sub-1");
    }

    @Test
    void rejectsTokenForAnotherApp() {
        assertInvalid(() -> verifier(CLIENT_ID).verify(token(googleKey, claims -> claims.audience("someone-else"))));
    }

    @Test
    void rejectsTokenNotIssuedByGoogle() {
        assertInvalid(() -> verifier(CLIENT_ID).verify(token(googleKey, claims -> claims.issuer("https://evil.example"))));
    }

    @Test
    void rejectsExpiredToken() {
        Instant past = Instant.now().minusSeconds(7200);
        assertInvalid(() -> verifier(CLIENT_ID).verify(token(googleKey, claims -> claims
                .issueTime(Date.from(past)).expirationTime(Date.from(past.plusSeconds(3600))))));
    }

    @Test
    void rejectsForgedSignature() {
        assertInvalid(() -> verifier(CLIENT_ID).verify(token(otherKey, claims -> claims)));
    }

    @Test
    void rejectsUnverifiedEmail() {
        assertInvalid(() -> verifier(CLIENT_ID).verify(token(googleKey, claims -> claims.claim("email_verified", false))));
    }

    @Test
    void isOffWithoutClientId() {
        assertThatThrownBy(() -> verifier("").verify("anything"))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.GOOGLE_LOGIN_DISABLED));
    }

    private static GoogleTokenVerifier verifier(String clientId) {
        return new GoogleTokenVerifier(new GoogleProperties(clientId), () -> {
            try {
                return NimbusJwtDecoder.withPublicKey(googleKey.toRSAPublicKey()).build();
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        });
    }

    private static String token(RSAKey signingKey, UnaryOperator<JWTClaimsSet.Builder> customise) {
        Instant now = Instant.now();
        JWTClaimsSet.Builder claims = new JWTClaimsSet.Builder()
                .issuer("https://accounts.google.com")
                .audience(CLIENT_ID)
                .subject("google-sub-1")
                .claim("email", "Anmol@Gmail.com")
                .claim("email_verified", true)
                .claim("name", "Anmol Mehla")
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plusSeconds(3600)));
        try {
            SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), customise.apply(claims).build());
            jwt.sign(new RSASSASigner(signingKey));
            return jwt.serialize();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static void assertInvalid(ThrowingCallable call) {
        assertThatThrownBy(call).isInstanceOfSatisfying(BusinessException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.GOOGLE_TOKEN_INVALID));
    }
}
