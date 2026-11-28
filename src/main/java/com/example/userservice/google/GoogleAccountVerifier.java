package com.example.userservice.google;

import com.example.userservice.common.UserServiceException;
import com.example.userservice.config.UserServiceProperties;
import com.google.common.base.Suppliers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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
import java.util.Locale;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Checks the ID token from the "Sign in with Google" button: Google's signature, issuer, our client id
 * as audience, expiry, and a verified email.
 */
@Component
public class GoogleAccountVerifier {

    static final String GOOGLE_KEYS = "https://www.googleapis.com/oauth2/v3/certs";
    private static final Set<String> ISSUERS = Set.of("https://accounts.google.com", "accounts.google.com");

    private final String clientId;
    private final Supplier<NimbusJwtDecoder> decoder;

    @Autowired
    GoogleAccountVerifier(UserServiceProperties properties) {
        this(properties.google().clientId(), () -> NimbusJwtDecoder.withJwkSetUri(GOOGLE_KEYS).build());
    }

    GoogleAccountVerifier(String clientId, Supplier<NimbusJwtDecoder> decoderFactory) {
        this.clientId = clientId;
        this.decoder = Suppliers.memoize(() -> {
            NimbusJwtDecoder built = decoderFactory.get();
            built.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                    JwtValidators.createDefault(),
                    new JwtClaimValidator<Object>(JwtClaimNames.ISS, iss -> iss != null && ISSUERS.contains(iss.toString())),
                    new JwtClaimValidator<Collection<?>>(JwtClaimNames.AUD, aud -> aud != null && aud.contains(clientId))));
            return built;
        });
    }

    public boolean enabled() {
        return StringUtils.hasText(clientId);
    }

    public GoogleIdentity verify(String credential) {
        if (!enabled()) {
            throw new UserServiceException(HttpStatus.SERVICE_UNAVAILABLE, "Google login is not set up");
        }
        Jwt jwt;
        try {
            jwt = decoder.get().decode(credential);
        } catch (JwtException e) {
            throw UserServiceException.unauthorized("Google sign in failed, try again");
        }
        String email = jwt.getClaimAsString("email");
        if (email == null || !"true".equalsIgnoreCase(String.valueOf(jwt.getClaims().get("email_verified")))) {
            throw UserServiceException.unauthorized("Your Google account email is not verified");
        }
        return new GoogleIdentity(email.trim().toLowerCase(Locale.ROOT), jwt.getClaimAsString("name"));
    }

    public record GoogleIdentity(String email, String name) {
    }
}
