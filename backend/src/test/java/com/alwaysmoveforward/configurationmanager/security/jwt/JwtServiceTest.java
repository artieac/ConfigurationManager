package com.alwaysmoveforward.configurationmanager.security.jwt;

import com.alwaysmoveforward.configurationmanager.domainmodel.Role;
import com.alwaysmoveforward.configurationmanager.domainmodel.User;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTest {

    private static final String SIGNING_KEY = Base64.getEncoder().encodeToString(new byte[32]);

    private final User user = new User(42L, "auth0|abc123", "user@example.com", "Test User",
            Role.READ_WRITE, true, Instant.now(), Instant.now());

    @Test
    void issuedTokenParsesBackToTheSamePrincipal() {
        JwtService jwtService = new JwtService(new JwtProperties(SIGNING_KEY, 60, "sm_auth", true, ""));

        String token = jwtService.issueToken(user);
        Optional<AuthenticatedPrincipal> parsed = jwtService.parseAndValidate(token);

        assertTrue(parsed.isPresent());
        assertEquals(user.id(), parsed.get().userId());
        assertEquals(user.email(), parsed.get().email());
        assertEquals(user.role(), parsed.get().role());
    }

    @Test
    void expiredTokenFailsValidation() {
        JwtService jwtService = new JwtService(new JwtProperties(SIGNING_KEY, -5, "sm_auth", true, ""));

        String token = jwtService.issueToken(user);

        assertTrue(jwtService.parseAndValidate(token).isEmpty());
    }

    @Test
    void tokenSignedWithADifferentKeyFailsValidation() {
        byte[] differentKeyBytes = new byte[32];
        java.util.Arrays.fill(differentKeyBytes, (byte) 7);

        JwtService issuer = new JwtService(new JwtProperties(SIGNING_KEY, 60, "sm_auth", true, ""));
        JwtService verifier = new JwtService(new JwtProperties(
                Base64.getEncoder().encodeToString(differentKeyBytes), 60, "sm_auth", true, ""));

        String token = issuer.issueToken(user);

        assertTrue(verifier.parseAndValidate(token).isEmpty());
    }
}

