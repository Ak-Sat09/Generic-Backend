package com.example.userservice.google;

import com.example.userservice.common.UserServiceException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import java.time.Instant;
import java.util.Date;
import java.util.function.UnaryOperator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Signs tokens with a local key in place of Google's, so the real checks run offline. */
class GoogleAccountVerifierTest {

    private static final String CLIENT_ID = "client-1.apps.googleusercontent.com";
    private static RSAKey googleKey;

    @BeforeAll
    static void key() throws Exception {
        googleKey = new RSAKeyGenerator(2048).generate();
    }

    @Test
    void acceptsAValidToken() {
        GoogleAccountVerifier.GoogleIdentity identity = verifier(CLIENT_ID).verify(token(c -> c));

        assertThat(identity.email()).isEqualTo("anmol@gmail.com");
        assertThat(identity.name()).isEqualTo("Anmol Mehla");
    }

    @Test
    void rejectsOtherAudienceIssuerAndUnverifiedEmail() {
        GoogleAccountVerifier verifier = verifier(CLIENT_ID);

        assertThatThrownBy(() -> verifier.verify(token(c -> c.audience("other")))).isInstanceOf(UserServiceException.class);
        assertThatThrownBy(() -> verifier.verify(token(c -> c.issuer("https://evil.example")))).isInstanceOf(UserServiceException.class);
        assertThatThrownBy(() -> verifier.verify(token(c -> c.claim("email_verified", false)))).isInstanceOf(UserServiceException.class);
    }

    @Test
    void isOffWithoutClientId() {
        GoogleAccountVerifier off = verifier("");

        assertThat(off.enabled()).isFalse();
        assertThatThrownBy(() -> off.verify("x"))
                .isInstanceOfSatisfying(UserServiceException.class, e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));
    }

    private static GoogleAccountVerifier verifier(String clientId) {
        return new GoogleAccountVerifier(clientId, () -> {
            try {
                return NimbusJwtDecoder.withPublicKey(googleKey.toRSAPublicKey()).build();
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        });
    }

    private static String token(UnaryOperator<JWTClaimsSet.Builder> customise) {
        Instant now = Instant.now();
        JWTClaimsSet.Builder claims = new JWTClaimsSet.Builder()
                .issuer("https://accounts.google.com").audience(CLIENT_ID).subject("sub-1")
                .claim("email", "Anmol@Gmail.com").claim("email_verified", true).claim("name", "Anmol Mehla")
                .issueTime(Date.from(now)).expirationTime(Date.from(now.plusSeconds(3600)));
        try {
            SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), customise.apply(claims).build());
            jwt.sign(new RSASSASigner(googleKey));
            return jwt.serialize();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
