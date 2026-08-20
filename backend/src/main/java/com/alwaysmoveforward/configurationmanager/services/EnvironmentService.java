package com.alwaysmoveforward.configurationmanager.services;

import com.alwaysmoveforward.configurationmanager.data.repositories.EnvironmentRepository;
import com.alwaysmoveforward.configurationmanager.domainmodel.Environment;
import com.alwaysmoveforward.configurationmanager.exceptions.NotFoundException;
import com.alwaysmoveforward.configurationmanager.security.jwt.AuthenticatedPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EnvironmentService extends ServiceBase {

    private final EnvironmentRepository environmentRepository;
    private final ConfigurationService configurationService;

    public EnvironmentService(EnvironmentRepository environmentRepository, ConfigurationService configurationService) {
        this.environmentRepository = environmentRepository;
        this.configurationService = configurationService;
    }

    public List<Environment> listEnvironments(Long systemId) {
        return environmentRepository.findBySystemId(systemId);
    }

    public Environment getEnvironment(Long id) {
        return environmentRepository.findById(id);
    }

    /** Looked up by external id (scoped to one system) — used by the API-key bulk-reveal endpoint. */
    public Environment getEnvironmentByExternalId(Long systemId, String externalId) {
        return environmentRepository.findBySystemIdAndExternalId(systemId, externalId)
                .orElseThrow(() -> new NotFoundException("Environment '" + externalId + "' not found in this system"));
    }

    public Environment createEnvironment(Long systemId, String name, String externalId, AuthenticatedPrincipal principal) {
        return environmentRepository.create(
                Environment.newEnvironment(systemId, name, resolveExternalId(externalId, name), changeMetadataFor(principal)),
                principal.userId());
    }

    public Environment renameEnvironment(Long id, String newName, String newExternalId, AuthenticatedPrincipal principal) {
        Environment existing = environmentRepository.findById(id);
        return environmentRepository.update(
                existing.withRename(newName, resolveExternalId(newExternalId, newName), changeMetadataFor(principal)), principal.userId());
    }

    /** External id defaults to the name whenever it's left blank — see EnvironmentRequest. */
    private String resolveExternalId(String providedExternalId, String name) {
        return (providedExternalId == null || providedExternalId.isBlank()) ? name : providedExternalId;
    }

    /** Cascades through {@link ConfigurationService} first so every secret's value in this environment gets its own DELETED history entry. */
    @Transactional
    public void deleteEnvironment(Long id, AuthenticatedPrincipal principal) {
        configurationService.deleteAllValuesForEnvironment(id, principal);
        environmentRepository.delete(id);
    }
}

