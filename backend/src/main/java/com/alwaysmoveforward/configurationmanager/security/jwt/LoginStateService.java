package com.alwaysmoveforward.configurationmanager.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

/**
 * Issues and verifies the OAuth {@code state} value used on the Auth0 login
 * redirect. It's a short-lived, self-verifying signed token rather than a
 * server-stored value — a fresh one is generated per login attempt and
 * validated purely by its signature and expiry on the way back, so the
 * backend never needs a session or a cookie to protect the login flow
 * against CSRF (an attacker completing their own Auth0 login and tricking a
 * victim into hitting the callback with the attacker's code).
 *
 * Reuses the same HMAC key as {@link JwtService} but stamps a distinct
 * {@code purpose} claim so a login-state token can never be mistaken for, or
 * substituted as, a session token (and vice versa).
 */
@Service
public class LoginStateService {

    private static final String CLAIM_PURPOSE = "purpose";
    private static final String PURPOSE_LOGIN_STATE = "login_state";
    private static final Duration VALIDITY = Duration.ofMinutes(10);

    private final SecretKey key;
    private final SecureRandom secureRandom = new SecureRandom();

    public LoginStateService(JwtProperties properties) {
        this.key = properties.toSigningKey();
    }

    public String issueState() {
        byte[] nonce = new byte[16];
        secureRandom.nextBytes(nonce);
        Instant now = Instant.now();

        return Jwts.builder()
                .subject(Base64.getUrlEncoder().withoutPadding().encodeToString(nonce))
                .claim(CLAIM_PURPOSE, PURPOSE_LOGIN_STATE)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(VALIDITY)))
                .signWith(key)
                .compact();
    }

    /** True only for a state value this service issued, unexpired and untampered with. */
    public boolean isValid(String state) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(state)
                    .getPayload();

            return PURPOSE_LOGIN_STATE.equals(claims.get(CLAIM_PURPOSE, String.class));
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}


