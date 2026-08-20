package com.alwaysmoveforward.configurationmanager.security.apikey;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Generates and hashes API key tokens. Only the hash is ever persisted (see
 * {@code ApiKeyRepository}) — the raw token is shown to the caller exactly
 * once, at creation, and this class never stores or logs it. SHA-256 (not a
 * slow password hash like bcrypt) is the right tool here: the token itself
 * has 256 bits of entropy from {@link SecureRandom}, so — unlike a
 * human-chosen password — there's nothing for a slow hash to protect against;
 * a fast hash keeps every authenticated request cheap.
 */
@Component
public class ApiKeyCrypto {

    private static final String TOKEN_PREFIX = "smk_";
    private static final int TOKEN_RANDOM_BYTES = 32;

    private final SecureRandom secureRandom = new SecureRandom();

    public String generateToken() {
        byte[] randomBytes = new byte[TOKEN_RANDOM_BYTES];
        secureRandom.nextBytes(randomBytes);
        return TOKEN_PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    public String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is a standard JDK algorithm — this can't happen on any real JVM.
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}

