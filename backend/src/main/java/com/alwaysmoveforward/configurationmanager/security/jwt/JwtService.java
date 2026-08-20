package com.alwaysmoveforward.configurationmanager.security.jwt;

import com.alwaysmoveforward.configurationmanager.domainmodel.Role;
import com.alwaysmoveforward.configurationmanager.domainmodel.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.http.Cookie;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

/**
 * Issues and verifies this application's own signed session token (HMAC-SHA256)
 * — a separate credential from anything Auth0 issues. Auth0 is only consulted
 * during login; every subsequent request is authenticated purely against this
 * cookie so the backend never needs to call out to Auth0 per-request.
 */
@Service
public class JwtService {

    private static final Logger LOG = LoggerFactory.getLogger(JwtService.class);

    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_DISPLAY_NAME = "displayName";
    private static final String CLAIM_ROLE = "role";

    private final SecretKey key;
    private final JwtProperties properties;

    public JwtService(JwtProperties properties) {
        this.key = properties.toSigningKey();
        this.properties = properties;
    }

    public String issueToken(User user) {
        Instant now = Instant.now();
        Instant expiry = now.plus(Duration.ofMinutes(properties.expirationMinutes()));

        return Jwts.builder()
                .subject(String.valueOf(user.id()))
                .claim(CLAIM_EMAIL, user.email())
                .claim(CLAIM_DISPLAY_NAME, user.displayName())
                .claim(CLAIM_ROLE, user.role().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(key)
                .compact();
    }

    public Optional<AuthenticatedPrincipal> parseAndValidate(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return Optional.of(new AuthenticatedPrincipal(
                    Long.valueOf(claims.getSubject()),
                    claims.get(CLAIM_EMAIL, String.class),
                    claims.get(CLAIM_DISPLAY_NAME, String.class),
                    Role.valueOf(claims.get(CLAIM_ROLE, String.class))));
        } catch (JwtException | IllegalArgumentException e) {
            // Never log the token itself — it's a credential. The exception type/message alone
            // (expired vs bad signature vs malformed) is enough to diagnose without exposing it.
            LOG.debug("Rejected session cookie: {}: {}", e.getClass().getSimpleName(), e.getMessage());
            return Optional.empty();
        }
    }

    public String cookieName() {
        return properties.cookieName();
    }

    public boolean cookieSecure() {
        return properties.cookieSecure();
    }

    public String cookieDomain() {
        return properties.cookieDomain();
    }

    public long expirationSeconds() {
        return Duration.ofMinutes(properties.expirationMinutes()).toSeconds();
    }


    public Cookie generateExpiredCookie(){
        Cookie retVal = new Cookie(this.cookieName(), "");
        retVal.setDomain(this.cookieDomain());
        retVal.setHttpOnly(true);
        retVal.setMaxAge(0);
        retVal.setSecure(this.cookieSecure());
        retVal.setPath("/");

        return retVal;
    }
}


