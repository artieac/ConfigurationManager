package com.alwaysmoveforward.configurationmanager.security.jwt;

import com.alwaysmoveforward.configurationmanager.domainmodel.Role;
import com.alwaysmoveforward.configurationmanager.domainmodel.User;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoginStateServiceTest {

    private static final String SIGNING_KEY = Base64.getEncoder().encodeToString(new byte[32]);
    private static final JwtProperties PROPERTIES = new JwtProperties(SIGNING_KEY, 60, "sm_auth", true, "");

    @Test
    void issuedStateValidatesSuccessfully() {
        LoginStateService loginStateService = new LoginStateService(PROPERTIES);

        String state = loginStateService.issueState();

        assertTrue(loginStateService.isValid(state));
    }

    @Test
    void expiredStateFailsValidation() {
        LoginStateService loginStateService = new LoginStateService(PROPERTIES);

        Instant past = Instant.now().minusSeconds(3600);
        String expiredState = Jwts.builder()
                .subject("nonce")
                .claim("purpose", "login_state")
                .issuedAt(Date.from(past.minusSeconds(60)))
                .expiration(Date.from(past))
                .signWith(PROPERTIES.toSigningKey())
                .compact();

        assertFalse(loginStateService.isValid(expiredState));
    }

    @Test
    void stateSignedWithADifferentKeyFailsValidation() {
        byte[] differentKeyBytes = new byte[32];
        Arrays.fill(differentKeyBytes, (byte) 7);

        LoginStateService issuer = new LoginStateService(PROPERTIES);
        LoginStateService verifier = new LoginStateService(
                new JwtProperties(Base64.getEncoder().encodeToString(differentKeyBytes), 60, "sm_auth", true, ""));

        String state = issuer.issueState();

        assertFalse(verifier.isValid(state));
    }

    @Test
    void aSessionTokenIsNotAcceptedAsLoginState() {
        LoginStateService loginStateService = new LoginStateService(PROPERTIES);
        JwtService jwtService = new JwtService(PROPERTIES);

        User user = new User(42L, "auth0|abc123", "user@example.com", "Test User",
                Role.READ_WRITE, true, Instant.now(), Instant.now());
        String sessionToken = jwtService.issueToken(user);

        assertFalse(loginStateService.isValid(sessionToken));
    }

    @Test
    void arbitraryStringFailsValidation() {
        LoginStateService loginStateService = new LoginStateService(PROPERTIES);

        assertFalse(loginStateService.isValid("not-a-jwt"));
    }
}

