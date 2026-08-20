package com.alwaysmoveforward.configurationmanager.security.jwt;

import io.jsonwebtoken.security.Keys;
import org.springframework.boot.context.properties.ConfigurationProperties;

import javax.crypto.SecretKey;
import java.util.Base64;

/** {@code signingKey} is a base64-encoded HMAC-SHA256 key, minimum 256 bits. */
@ConfigurationProperties(prefix = "configuration-manager.jwt")
public record JwtProperties(String signingKey, long expirationMinutes, String cookieName,
                             boolean cookieSecure, String cookieDomain) {

    /**
     * Decodes {@link #signingKey()} into a usable HMAC key. Shared by every
     * JWT-based signer in this package ({@link JwtService}, {@link LoginStateService})
     * so the same key material and validation live in exactly one place.
     */
    public SecretKey toSigningKey() {
        if (signingKey == null || signingKey.isBlank()) {
            throw new IllegalStateException(
                    "configuration-manager.jwt.signing-key (env JWT_SIGNING_KEY) must be set to a base64-encoded key of at least 256 bits");
        }
        byte[] keyBytes = Base64.getDecoder().decode(signingKey);
        if (keyBytes.length < 32) {
            throw new IllegalStateException("configuration-manager.jwt.signing-key must decode to at least 32 bytes");
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }
}


