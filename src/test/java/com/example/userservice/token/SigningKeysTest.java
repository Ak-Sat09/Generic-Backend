package com.example.userservice.token;

import org.junit.jupiter.api.Test;

import java.security.KeyPairGenerator;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SigningKeysTest {

    @Test
    void refusesToStartWithoutAKey() {
        assertThatThrownBy(() -> new SigningKeys("", false))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("USER_JWT_PRIVATE_KEY");
    }

    @Test
    void loadsBase64AndPemKeys() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        String base64 = Base64.getEncoder().encodeToString(generator.generateKeyPair().getPrivate().getEncoded());
        String pem = "-----BEGIN PRIVATE KEY-----\n" + base64.replaceAll("(.{64})", "$1\n") + "\n-----END PRIVATE KEY-----\n";

        SigningKeys fromBase64 = new SigningKeys(base64, false);
        SigningKeys fromPem = new SigningKeys(pem, false);

        assertThat(fromPem.keyId()).isEqualTo(fromBase64.keyId());
        assertThat(fromBase64.publicKey().getModulus()).isEqualTo(fromBase64.privateKey().getModulus());
    }

    @Test
    void rejectsGarbage() {
        assertThatThrownBy(() -> new SigningKeys("bm90LWEta2V5", false)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void publishesAnRsaJwk() {
        SigningKeys keys = new SigningKeys(null, true);

        assertThat(keys.publicJwk())
                .containsEntry("kty", "RSA")
                .containsEntry("alg", "RS256")
                .containsEntry("kid", keys.keyId())
                .containsEntry("e", "AQAB")
                .containsKey("n");
    }
}
