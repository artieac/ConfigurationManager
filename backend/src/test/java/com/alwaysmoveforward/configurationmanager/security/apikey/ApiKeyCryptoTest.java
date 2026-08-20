package com.alwaysmoveforward.configurationmanager.security.apikey;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiKeyCryptoTest {

    private final ApiKeyCrypto apiKeyCrypto = new ApiKeyCrypto();

    @Test
    void generatedTokensAreDistinctAndRecognizable() {
        String first = apiKeyCrypto.generateToken();
        String second = apiKeyCrypto.generateToken();

        assertNotEquals(first, second);
        assertTrue(first.startsWith("smk_"));
        assertTrue(second.startsWith("smk_"));
    }

    @Test
    void hashingTheSameTokenTwiceProducesTheSameHash() {
        String token = apiKeyCrypto.generateToken();

        assertEquals(apiKeyCrypto.hash(token), apiKeyCrypto.hash(token));
    }

    @Test
    void hashingDifferentTokensProducesDifferentHashes() {
        String first = apiKeyCrypto.generateToken();
        String second = apiKeyCrypto.generateToken();

        assertNotEquals(apiKeyCrypto.hash(first), apiKeyCrypto.hash(second));
    }

    @Test
    void hashIsA64CharacterHexString() {
        String hash = apiKeyCrypto.hash(apiKeyCrypto.generateToken());

        assertEquals(64, hash.length());
        assertTrue(hash.matches("[0-9a-f]{64}"));
    }
}

