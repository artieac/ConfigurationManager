package com.alwaysmoveforward.configurationmanager.services;

import com.alwaysmoveforward.configurationmanager.data.repositories.ApiKeyRepository;
import com.alwaysmoveforward.configurationmanager.domainmodel.ApiKey;
import com.alwaysmoveforward.configurationmanager.domainmodel.ChangeMetadata;
import com.alwaysmoveforward.configurationmanager.domainmodel.Role;
import com.alwaysmoveforward.configurationmanager.exceptions.NotFoundException;
import com.alwaysmoveforward.configurationmanager.security.apikey.ApiKeyCrypto;
import com.alwaysmoveforward.configurationmanager.security.jwt.AuthenticatedPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ApiKeyServiceTest {

    private static final Long SYSTEM_ID = 1L;
    private static final AuthenticatedPrincipal PRINCIPAL =
            new AuthenticatedPrincipal(9L, "admin@example.com", "Admin User", Role.ADMIN);

    private ApiKeyRepository apiKeyRepository;
    private ApiKeyCrypto apiKeyCrypto;
    private ApiKeyService apiKeyService;

    @BeforeEach
    void setUp() {
        apiKeyRepository = mock(ApiKeyRepository.class);
        apiKeyCrypto = mock(ApiKeyCrypto.class);
        apiKeyService = new ApiKeyService(apiKeyRepository, apiKeyCrypto);
    }

    @Test
    void createApiKeyPersistsOnlyTheHashButReturnsTheRawTokenOnce() {
        when(apiKeyCrypto.generateToken()).thenReturn("smk_rawtoken");
        when(apiKeyCrypto.hash("smk_rawtoken")).thenReturn("hashed-value");

        ApiKey saved = new ApiKey(50L, SYSTEM_ID, "CI Pipeline",
                new ChangeMetadata(PRINCIPAL.userId(), PRINCIPAL.displayName(), Instant.now()), null);
        when(apiKeyRepository.create(any(), eq("hashed-value"), eq(PRINCIPAL.userId()))).thenReturn(saved);

        ApiKeyService.IssuedApiKey issued = apiKeyService.createApiKey(SYSTEM_ID, "CI Pipeline", PRINCIPAL);

        assertEquals("smk_rawtoken", issued.token());
        assertEquals(saved, issued.apiKey());

        ArgumentCaptor<ApiKey> captor = ArgumentCaptor.forClass(ApiKey.class);
        verify(apiKeyRepository).create(captor.capture(), eq("hashed-value"), eq(PRINCIPAL.userId()));
        assertEquals("CI Pipeline", captor.getValue().name());
        assertEquals(SYSTEM_ID, captor.getValue().systemId());
    }

    @Test
    void renameApiKeyRenamesWhenItBelongsToTheClaimedSystem() {
        ApiKey key = new ApiKey(50L, SYSTEM_ID, "Old Name", new ChangeMetadata(1L, "Someone", Instant.now()), null);
        ApiKey renamed = new ApiKey(50L, SYSTEM_ID, "New Name", key.created(), null);
        when(apiKeyRepository.findById(50L)).thenReturn(key);
        when(apiKeyRepository.update(any(ApiKey.class))).thenReturn(renamed);

        ApiKey result = apiKeyService.renameApiKey(SYSTEM_ID, 50L, "New Name");

        assertEquals("New Name", result.name());
        verify(apiKeyRepository).update(any(ApiKey.class));
    }

    @Test
    void renameApiKeyRejectsAKeyBelongingToADifferentSystem() {
        ApiKey key = new ApiKey(50L, 999L, "CI Pipeline", new ChangeMetadata(1L, "Someone", Instant.now()), null);
        when(apiKeyRepository.findById(50L)).thenReturn(key);

        assertThrows(NotFoundException.class, () -> apiKeyService.renameApiKey(SYSTEM_ID, 50L, "New Name"));
        verify(apiKeyRepository, never()).update(any(ApiKey.class));
    }

    @Test
    void revokeApiKeyDeletesWhenItBelongsToTheClaimedSystem() {
        ApiKey key = new ApiKey(50L, SYSTEM_ID, "CI Pipeline", new ChangeMetadata(1L, "Someone", Instant.now()), null);
        when(apiKeyRepository.findById(50L)).thenReturn(key);

        apiKeyService.revokeApiKey(SYSTEM_ID, 50L);

        verify(apiKeyRepository).delete(50L);
    }

    @Test
    void revokeApiKeyRejectsAKeyBelongingToADifferentSystem() {
        ApiKey key = new ApiKey(50L, 999L, "CI Pipeline", new ChangeMetadata(1L, "Someone", Instant.now()), null);
        when(apiKeyRepository.findById(50L)).thenReturn(key);

        assertThrows(NotFoundException.class, () -> apiKeyService.revokeApiKey(SYSTEM_ID, 50L));
        verify(apiKeyRepository, never()).delete(any());
    }
}

