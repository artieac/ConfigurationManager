package com.alwaysmoveforward.configurationmanager.services;

import com.alwaysmoveforward.configurationmanager.data.repositories.SystemHistoryRepository;
import com.alwaysmoveforward.configurationmanager.data.repositories.SystemRepository;
import com.alwaysmoveforward.configurationmanager.domainmodel.HistoryAction;
import com.alwaysmoveforward.configurationmanager.domainmodel.Configuration;
import com.alwaysmoveforward.configurationmanager.domainmodel.ConfigurationSystem;
import com.alwaysmoveforward.configurationmanager.domainmodel.SystemHistoryEntry;
import com.alwaysmoveforward.configurationmanager.exceptions.NotFoundException;
import com.alwaysmoveforward.configurationmanager.security.jwt.AuthenticatedPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Every write path here also appends a {@link SystemHistoryEntry} in the same
 * transaction — history is not best-effort logging, it's part of the write.
 */
@Service
public class SystemService extends ServiceBase {

    private final SystemRepository systemRepository;
    private final SystemHistoryRepository systemHistoryRepository;
    private final ConfigurationService configurationService;

    public SystemService(SystemRepository systemRepository, SystemHistoryRepository systemHistoryRepository,
                          ConfigurationService configurationService) {
        this.systemRepository = systemRepository;
        this.systemHistoryRepository = systemHistoryRepository;
        this.configurationService = configurationService;
    }

    public List<ConfigurationSystem> listSystems() {
        return systemRepository.findAll();
    }

    public ConfigurationSystem getSystem(Long id) {
        return systemRepository.findById(id);
    }

    /** Looked up by external id (globally unique) — used by the API-key bulk-reveal endpoint. */
    public ConfigurationSystem getSystemByExternalId(String externalId) {
        return systemRepository.findByExternalId(externalId)
                .orElseThrow(() -> new NotFoundException("System '" + externalId + "' not found"));
    }

    public List<SystemHistoryEntry> getHistory(Long systemId) {
        return systemHistoryRepository.findBySystemId(systemId);
    }

    @Transactional
    public ConfigurationSystem createSystem(String name, String externalId, String description, AuthenticatedPrincipal principal) {
        ConfigurationSystem created = systemRepository.create(
                ConfigurationSystem.newSystem(name, resolveExternalId(externalId, name), description, changeMetadataFor(principal)),
                principal.userId());
        recordHistory(created, HistoryAction.CREATED, principal);
        return created;
    }

    @Transactional
    public ConfigurationSystem updateSystem(Long id, String newName, String newExternalId, String newDescription, AuthenticatedPrincipal principal) {
        ConfigurationSystem existing = systemRepository.findById(id);
        ConfigurationSystem updated = systemRepository.update(
                existing.withUpdatedDetails(newName, resolveExternalId(newExternalId, newName), newDescription));
        recordHistory(updated, HistoryAction.UPDATED, principal);
        return updated;
    }

    /** External id defaults to the name whenever it's left blank — see SystemRequest. */
    private String resolveExternalId(String providedExternalId, String name) {
        return (providedExternalId == null || providedExternalId.isBlank()) ? name : providedExternalId;
    }

    /**
     * Deletes every secret in the system first (via {@link ConfigurationService}, so
     * each one gets its own DELETED history entry) before removing the system
     * itself — the audit trail must not silently disappear behind a DB cascade.
     */
    @Transactional
    public void deleteSystem(Long id, AuthenticatedPrincipal principal) {
        ConfigurationSystem existing = systemRepository.findById(id);

        List<Configuration> secrets = configurationService.listConfigurations(id);
        for (Configuration secret : secrets) {
            configurationService.deleteConfiguration(secret.id(), principal);
        }

        recordHistory(existing, HistoryAction.DELETED, principal);
        systemRepository.delete(id);
    }

    private void recordHistory(ConfigurationSystem system, HistoryAction action, AuthenticatedPrincipal principal) {
        SystemHistoryEntry entry = SystemHistoryEntry.recordChange(system, action, changeMetadataFor(principal));
        systemHistoryRepository.record(entry, principal.userId());
    }
}

