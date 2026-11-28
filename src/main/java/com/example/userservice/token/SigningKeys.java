package com.example.userservice.token;

import com.example.userservice.config.UserServiceProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The RSA key pair for access tokens. Only the User Service holds the private key; everyone else
 * checks tokens with the public key, published as a JWK. A missing key stops the app from starting
 * unless {@code generate-if-missing} is on (local runs and tests).
 */
@Component
public class SigningKeys {

    private static final Logger log = LoggerFactory.getLogger(SigningKeys.class);

    private final RSAPrivateKey privateKey;
    private final RSAPublicKey publicKey;
    private final String keyId;

    @Autowired
    SigningKeys(UserServiceProperties properties) {
        this(properties.jwt().privateKey(), properties.jwt().generateIfMissing());
    }

    SigningKeys(String encodedPrivateKey, boolean generateIfMissing) {
        KeyPair pair;
        if (StringUtils.hasText(encodedPrivateKey)) {
            pair = load(encodedPrivateKey);
        } else if (generateIfMissing) {
            log.warn("USER_JWT_PRIVATE_KEY is not set: using a temporary key. Tokens stop working after a restart.");
            pair = generate();
        } else {
            throw new IllegalStateException("USER_JWT_PRIVATE_KEY must be set (base64 PKCS#8 RSA key). "
                    + "Set USER_JWT_GENERATE_IF_MISSING=true only for local runs.");
        }
        this.privateKey = (RSAPrivateKey) pair.getPrivate();
        this.publicKey = (RSAPublicKey) pair.getPublic();
        this.keyId = thumbprint(publicKey);
    }

    public RSAPrivateKey privateKey() {
        return privateKey;
    }

    public RSAPublicKey publicKey() {
        return publicKey;
    }

    public String keyId() {
        return keyId;
    }

    /** The public key in JWK form (RFC 7517), for /.well-known/jwks.json. */
    public Map<String, Object> publicJwk() {
        Map<String, Object> jwk = new LinkedHashMap<>();
        jwk.put("kty", "RSA");
        jwk.put("use", "sig");
        jwk.put("alg", "RS256");
        jwk.put("kid", keyId);
        jwk.put("n", base64Url(publicKey.getModulus()));
        jwk.put("e", base64Url(publicKey.getPublicExponent()));
        return jwk;
    }

    private static KeyPair load(String encoded) {
        String body = encoded.replaceAll("-----[A-Z ]+-----", "").replaceAll("\\s", "");
        try {
            KeyFactory factory = KeyFactory.getInstance("RSA");
            RSAPrivateCrtKey key = (RSAPrivateCrtKey) factory.generatePrivate(
                    new PKCS8EncodedKeySpec(Base64.getDecoder().decode(body)));
            RSAPublicKey pub = (RSAPublicKey) factory.generatePublic(
                    new RSAPublicKeySpec(key.getModulus(), key.getPublicExponent()));
            if (key.getModulus().bitLength() < 2048) {
                throw new IllegalStateException("USER_JWT_PRIVATE_KEY must be at least 2048 bits");
            }
            return new KeyPair(pub, key);
        } catch (GeneralSecurityException | IllegalArgumentException | ClassCastException e) {
            throw new IllegalStateException("USER_JWT_PRIVATE_KEY is not a valid PKCS#8 RSA private key", e);
        }
    }

    private static KeyPair generate() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Could not generate an RSA key", e);
        }
    }

    private static String thumbprint(RSAPublicKey key) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(key.getEncoded());
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest).substring(0, 16);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String base64Url(BigInteger value) {
        byte[] bytes = value.toByteArray();
        if (bytes.length > 1 && bytes[0] == 0) {
            bytes = Arrays.copyOfRange(bytes, 1, bytes.length);
        }
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
