package com.alwaysmoveforward.configurationmanager.services;

import com.alwaysmoveforward.configurationmanager.data.repositories.ApiKeyRepository;
import com.alwaysmoveforward.configurationmanager.domainmodel.ApiKey;
import com.alwaysmoveforward.configurationmanager.exceptions.NotFoundException;
import com.alwaysmoveforward.configurationmanager.security.apikey.ApiKeyCrypto;
import com.alwaysmoveforward.configurationmanager.security.jwt.AuthenticatedPrincipal;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Admin-facing management of API keys (see {@code ApiKeyAuthenticationFilter}
 * for how they're used to authenticate). Creating one hands back the raw
 * token exactly once — nothing after this point can recover it.
 */
@Service
public class ApiKeyService extends ServiceBase {

    private final ApiKeyRepository apiKeyRepository;
    private final ApiKeyCrypto apiKeyCrypto;

    public ApiKeyService(ApiKeyRepository apiKeyRepository, ApiKeyCrypto apiKeyCrypto) {
        this.apiKeyRepository = apiKeyRepository;
        this.apiKeyCrypto = apiKeyCrypto;
    }

    public record IssuedApiKey(ApiKey apiKey, String token) {
    }

    public List<ApiKey> listApiKeys(Long systemId) {
        return apiKeyRepository.findBySystemId(systemId);
    }

    public IssuedApiKey createApiKey(Long systemId, String name, AuthenticatedPrincipal principal) {
        String token = apiKeyCrypto.generateToken();
        String tokenHash = apiKeyCrypto.hash(token);

        ApiKey saved = apiKeyRepository.create(
                ApiKey.newKey(systemId, name, changeMetadataFor(principal)), tokenHash, principal.userId());
        return new IssuedApiKey(saved, token);
    }

    public ApiKey renameApiKey(Long systemId, Long apiKeyId, String newName) {
        ApiKey apiKey = apiKeyRepository.findById(apiKeyId);
        if (!apiKey.systemId().equals(systemId)) {
            throw new NotFoundException("API key " + apiKeyId + " does not belong to system " + systemId);
        }
        return apiKeyRepository.update(apiKey.withName(newName));
    }

    public void revokeApiKey(Long systemId, Long apiKeyId) {
        ApiKey apiKey = apiKeyRepository.findById(apiKeyId);
        if (!apiKey.systemId().equals(systemId)) {
            throw new NotFoundException("API key " + apiKeyId + " does not belong to system " + systemId);
        }
        apiKeyRepository.delete(apiKeyId);
    }
}

