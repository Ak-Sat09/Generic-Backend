package com.buytogether.auth.google;

import com.buytogether.common.email.EmailAddress;
import com.buytogether.common.error.BusinessException;
import com.buytogether.common.error.ErrorCode;
import com.google.common.base.Suppliers;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Checks a Google ID token ("Sign in with Google" credential) the way Google documents it:
 * signature against Google's public keys, issuer, audience = our client id, not expired,
 * and a verified email. No Google SDK, just Spring Security's JWT decoder.
 */
@Slf4j
@Component
public class GoogleTokenVerifier {

    static final String GOOGLE_KEYS = "https://www.googleapis.com/oauth2/v3/certs";
    private static final Set<String> GOOGLE_ISSUERS = Set.of("https://accounts.google.com", "accounts.google.com");

    private final String clientId;
    private final Supplier<NimbusJwtDecoder> decoder;

    @Autowired
    GoogleTokenVerifier(GoogleProperties properties) {
        this(properties, () -> NimbusJwtDecoder.withJwkSetUri(GOOGLE_KEYS).build());
    }

    GoogleTokenVerifier(GoogleProperties properties, Supplier<NimbusJwtDecoder> decoderFactory) {
        this.clientId = properties.clientId();
        // built on first use, so the app starts even when Google is unreachable
        this.decoder = Suppliers.memoize(() -> {
            NimbusJwtDecoder built = decoderFactory.get();
            built.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                    JwtValidators.createDefault(),
                    new JwtClaimValidator<Object>(JwtClaimNames.ISS, iss -> iss != null && GOOGLE_ISSUERS.contains(iss.toString())),
                    new JwtClaimValidator<Collection<?>>(JwtClaimNames.AUD, aud -> aud != null && aud.contains(clientId))));
            return built;
        });
    }

    public GoogleIdentity verify(String idToken) {
        if (!StringUtils.hasText(clientId)) {
            throw new BusinessException(ErrorCode.GOOGLE_LOGIN_DISABLED);
        }
        Jwt jwt;
        try {
            jwt = decoder.get().decode(idToken);
        } catch (JwtException e) {
            log.info("Rejected Google token: {}", e.getMessage());
            throw new BusinessException(ErrorCode.GOOGLE_TOKEN_INVALID);
        }
        String email = jwt.getClaimAsString("email");
        if (email == null || !"true".equalsIgnoreCase(String.valueOf(jwt.getClaims().get("email_verified")))) {
            throw new BusinessException(ErrorCode.GOOGLE_TOKEN_INVALID, "Your Google account email is not verified");
        }
        return new GoogleIdentity(jwt.getSubject(), EmailAddress.parse(email), jwt.getClaimAsString("name"));
    }
}
