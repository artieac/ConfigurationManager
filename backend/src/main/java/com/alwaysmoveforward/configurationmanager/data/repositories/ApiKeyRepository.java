package com.alwaysmoveforward.configurationmanager.data.repositories;

import com.alwaysmoveforward.configurationmanager.data.Entities.ApiKeyEntity;
import com.alwaysmoveforward.configurationmanager.data.Entities.SystemEntity;
import com.alwaysmoveforward.configurationmanager.data.Entities.UserEntity;
import com.alwaysmoveforward.configurationmanager.data.dao.ApiKeyDAO;
import com.alwaysmoveforward.configurationmanager.data.dao.SystemDAO;
import com.alwaysmoveforward.configurationmanager.data.dao.UserDAO;
import com.alwaysmoveforward.configurationmanager.data.mapper.ApiKeyMapper;
import com.alwaysmoveforward.configurationmanager.domainmodel.ApiKey;
import com.alwaysmoveforward.configurationmanager.exceptions.NotFoundException;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public class ApiKeyRepository extends RepositoryBase {

    private final ApiKeyDAO apiKeyDAO;
    private final SystemDAO systemDAO;
    private final UserDAO userDAO;
    private final ApiKeyMapper apiKeyMapper;

    public ApiKeyRepository(ApiKeyDAO apiKeyDAO, SystemDAO systemDAO, UserDAO userDAO, ApiKeyMapper apiKeyMapper) {
        this.apiKeyDAO = apiKeyDAO;
        this.systemDAO = systemDAO;
        this.userDAO = userDAO;
        this.apiKeyMapper = apiKeyMapper;
    }

    public List<ApiKey> findBySystemId(Long systemId) {
        return apiKeyDAO.findBySystemIdOrderByNameAsc(systemId).stream().map(apiKeyMapper::toDomain).toList();
    }

    public ApiKey findById(Long id) {
        return orNotFound(apiKeyDAO.findById(id).map(apiKeyMapper::toDomain), () -> "API key not found: " + id);
    }

    /** Used only during authentication — the hash is the sole lookup key, never exposed via the domain model. */
    public Optional<ApiKey> findByTokenHash(String tokenHash) {
        return apiKeyDAO.findByTokenHash(tokenHash).map(apiKeyMapper::toDomain);
    }

    public ApiKey create(ApiKey apiKey, String tokenHash, Long createdByUserId) {
        SystemEntity system = systemDAO.findById(apiKey.systemId())
                .orElseThrow(() -> new NotFoundException("System not found: " + apiKey.systemId()));
        UserEntity createdBy = userDAO.findById(createdByUserId)
                .orElseThrow(() -> new NotFoundException("User not found: " + createdByUserId));

        ApiKeyEntity saved = apiKeyDAO.save(apiKeyMapper.toNewEntity(apiKey, tokenHash, system, createdBy));
        return apiKeyMapper.toDomain(saved);
    }

    public ApiKey update(ApiKey apiKey) {
        ApiKeyEntity existing = apiKeyDAO.findById(apiKey.id())
                .orElseThrow(() -> new NotFoundException("API key not found: " + apiKey.id()));
        apiKeyMapper.applyToEntity(existing, apiKey);
        return apiKeyMapper.toDomain(apiKeyDAO.save(existing));
    }

    public void delete(Long id) {
        if (!apiKeyDAO.existsById(id)) {
            throw new NotFoundException("API key not found: " + id);
        }
        apiKeyDAO.deleteById(id);
    }

    /** Best-effort usage tracking — called on every successful authentication. */
    public void touchLastUsed(Long id) {
        apiKeyDAO.findById(id).ifPresent(entity -> {
            entity.setLastUsedAt(Instant.now());
            apiKeyDAO.save(entity);
        });
    }
}

