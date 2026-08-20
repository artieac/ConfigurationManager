package com.alwaysmoveforward.configurationmanager.services;

import com.alwaysmoveforward.configurationmanager.data.repositories.EnvironmentRepository;
import com.alwaysmoveforward.configurationmanager.data.repositories.ConfigurationRepository;
import com.alwaysmoveforward.configurationmanager.data.repositories.ConfigurationValueHistoryRepository;
import com.alwaysmoveforward.configurationmanager.data.repositories.ConfigurationValueRepository;
import com.alwaysmoveforward.configurationmanager.data.repositories.SystemRepository;
import com.alwaysmoveforward.configurationmanager.domainmodel.EncryptedConfigurationValue;
import com.alwaysmoveforward.configurationmanager.domainmodel.Environment;
import com.alwaysmoveforward.configurationmanager.domainmodel.HistoryAction;
import com.alwaysmoveforward.configurationmanager.domainmodel.RevealedConfiguration;
import com.alwaysmoveforward.configurationmanager.domainmodel.Configuration;
import com.alwaysmoveforward.configurationmanager.domainmodel.ConfigurationSystem;
import com.alwaysmoveforward.configurationmanager.domainmodel.ConfigurationValue;
import com.alwaysmoveforward.configurationmanager.domainmodel.ConfigurationValueHistoryEntry;
import com.alwaysmoveforward.configurationmanager.exceptions.NotFoundException;
import com.alwaysmoveforward.configurationmanager.services.ConfigurationEncryptor;
import com.alwaysmoveforward.configurationmanager.security.jwt.AuthenticatedPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Owns the full lifecycle of a secret: its name, its per-environment encrypted
 * values, and their audit history. Merging what were previously two separate
 * services ({@code ConfigurationService} and {@code ConfigurationValueService}) eliminates
 * the circular service dependency and makes the aggregate boundary explicit —
 * all state changes to a secret and its values go through this single class.
 *
 * <p>Every write path that changes a value also appends a
 * {@link ConfigurationValueHistoryEntry} in the same transaction — history is not
 * best-effort logging, it is part of the write.
 */
@Service
public class ConfigurationService extends ServiceBase {

    private final ConfigurationRepository configurationRepository;
    private final ConfigurationValueRepository configurationValueRepository;
    private final ConfigurationValueHistoryRepository configurationValueHistoryRepository;
    private final SystemRepository systemRepository;
    private final EnvironmentRepository environmentRepository;
    private final ConfigurationEncryptor encryptionService;

    public ConfigurationService(ConfigurationRepository configurationRepository,
                         ConfigurationValueRepository configurationValueRepository,
                         ConfigurationValueHistoryRepository configurationValueHistoryRepository,
                         SystemRepository systemRepository,
                         EnvironmentRepository environmentRepository,
                         ConfigurationEncryptor encryptionService) {
        this.configurationRepository = configurationRepository;
        this.configurationValueRepository = configurationValueRepository;
        this.configurationValueHistoryRepository = configurationValueHistoryRepository;
        this.systemRepository = systemRepository;
        this.environmentRepository = environmentRepository;
        this.encryptionService = encryptionService;
    }

    // -------------------------------------------------------------------------
    // Configuration name operations
    // -------------------------------------------------------------------------

    public List<Configuration> listConfigurations(Long systemId) {
        return configurationRepository.findBySystemId(systemId);
    }

    public Configuration getConfiguration(Long id) {
        return configurationRepository.findById(id);
    }

    public Configuration createConfiguration(Long systemId, String name, AuthenticatedPrincipal principal) {
        return configurationRepository.create(Configuration.newConfiguration(systemId, name, changeMetadataFor(principal)), principal.userId());
    }

    public Configuration renameConfiguration(Long id, String newName, AuthenticatedPrincipal principal) {
        Configuration existing = configurationRepository.findById(id);
        return configurationRepository.update(existing.withRename(newName, changeMetadataFor(principal)), principal.userId());
    }

    /**
     * Deletes a secret and cascades through all of its per-environment values,
     * each generating its own DELETED history entry before the row is removed.
     */
    @Transactional
    public void deleteConfiguration(Long id, AuthenticatedPrincipal principal) {
        deleteAllValuesForConfiguration(id, principal);
        configurationRepository.delete(id);
    }

    // -------------------------------------------------------------------------
    // Configuration value operations
    // -------------------------------------------------------------------------

    /** Which environments (by id) currently have a value set for this secret — never the values themselves. */
    public List<ConfigurationValue> listValues(Long configurationId) {
        return configurationValueRepository.findByConfigurationId(configurationId);
    }

    /** Decrypts and returns the plaintext value. Caller (controller) must ensure this is never logged. */
    public String revealValue(Long configurationId, Long environmentId) {
        ConfigurationValue value = configurationValueRepository.findByConfigurationIdAndEnvironmentId(configurationId, environmentId)
                .orElseThrow(() -> new NotFoundException("No value set for this secret in that environment"));
        return encryptionService.decrypt(value.value());
    }

    /**
     * Bulk reveal — every secret that currently has a value set in this environment, decrypted. The
     * typical use is pulling a whole environment's config in one call (e.g. for a deploy). Caller
     * (controller) must ensure none of this is ever logged.
     *
     * <p>Uses a JOIN-fetching query ({@code findByEnvironmentIdWithConfiguration}) so the secret name is
     * loaded in a single query rather than N+1 individual lookups.
     */
    public List<RevealedConfiguration> revealAllForEnvironment(Long systemId, Long environmentId) {
        Environment environment = environmentRepository.findById(environmentId);
        if (!environment.systemId().equals(systemId)) {
            throw new NotFoundException("Environment " + environmentId + " does not belong to system " + systemId);
        }

        List<RevealedConfiguration> retVal = configurationValueRepository.findByEnvironmentIdWithConfiguration(environmentId).stream()
                .map(item -> new RevealedConfiguration(item.value().configurationId(), item.configurationName(), encryptionService.decrypt(item.value().value())))
                .toList();

        return retVal;
    }

    public List<ConfigurationValueHistoryEntry> getHistory(Long configurationId, Long environmentId) {
        return environmentId != null
                ? configurationValueHistoryRepository.findByConfigurationIdAndEnvironmentId(configurationId, environmentId)
                : configurationValueHistoryRepository.findByConfigurationId(configurationId);
    }

    /** Decrypts the value as it was at one specific history entry. Caller (controller) must ensure this is never logged. */
    public String revealHistoricValue(Long configurationId, Long historyId) {
        var snapshot = configurationValueHistoryRepository.findEncryptedSnapshot(configurationId, historyId)
                .orElseThrow(() -> new NotFoundException("No value snapshot for history entry " + historyId));
        return encryptionService.decrypt(snapshot);
    }

    /** Upserts the value for (secret, environment) — creates it if this is the first value set, updates it otherwise. */
    @Transactional
    public ConfigurationValue setValue(Long configurationId, Long environmentId, String plaintextValue, AuthenticatedPrincipal principal) {
        Configuration secret = configurationRepository.findById(configurationId);
        ConfigurationSystem system = systemRepository.findById(secret.systemId());
        Environment environment = environmentRepository.findById(environmentId);

        Optional<ConfigurationValue> existing = configurationValueRepository.findByConfigurationIdAndEnvironmentId(configurationId, environmentId);
        EncryptedConfigurationValue encrypted = encryptionService.encrypt(plaintextValue);

        ConfigurationValue saved;
        HistoryAction action;
        if (existing.isPresent()) {
            saved = configurationValueRepository.update(existing.get().withUpdatedValue(encrypted));
            action = HistoryAction.UPDATED;
        } else {
            saved = configurationValueRepository.create(
                    ConfigurationValue.newValue(configurationId, environmentId, encrypted, changeMetadataFor(principal)), principal.userId());
            action = HistoryAction.CREATED;
        }

        recordHistory(secret, system.name(), environment, action, saved.value(), principal);
        return saved;
    }

    @Transactional
    public void deleteValue(Long configurationId, Long environmentId, AuthenticatedPrincipal principal) {
        ConfigurationValue existing = configurationValueRepository.findByConfigurationIdAndEnvironmentId(configurationId, environmentId)
                .orElseThrow(() -> new NotFoundException("No value set for this secret in that environment"));
        Configuration secret = configurationRepository.findById(existing.configurationId());
        ConfigurationSystem system = systemRepository.findById(secret.systemId());
        deleteValueInternal(existing, secret, system, principal);
    }

    /**
     * Cascade used when a whole secret (name) is deleted — removes its value in every environment,
     * each with its own history entry. Pre-fetches the Configuration and System once for the entire loop
     * to avoid N+1 queries.
     */
    @Transactional
    public void deleteAllValuesForConfiguration(Long configurationId, AuthenticatedPrincipal principal) {
        Configuration secret = configurationRepository.findById(configurationId);
        ConfigurationSystem system = systemRepository.findById(secret.systemId());
        for (ConfigurationValue value : configurationValueRepository.findByConfigurationId(configurationId)) {
            deleteValueInternal(value, secret, system, principal);
        }
    }

    /**
     * Cascade used when a whole environment is deleted — removes every secret's value in it,
     * each with its own history entry.
     */
    @Transactional
    public void deleteAllValuesForEnvironment(Long environmentId, AuthenticatedPrincipal principal) {
        for (ConfigurationValue value : configurationValueRepository.findByEnvironmentId(environmentId)) {
            Configuration secret = configurationRepository.findById(value.configurationId());
            ConfigurationSystem system = systemRepository.findById(secret.systemId());
            deleteValueInternal(value, secret, system, principal);
        }
    }

    /**
     * Core delete primitive. Accepts pre-fetched {@code secret} and {@code system} so callers that
     * loop over multiple values for the same secret/system only pay the lookup cost once.
     */
    private void deleteValueInternal(ConfigurationValue value, Configuration secret, ConfigurationSystem system, AuthenticatedPrincipal principal) {
        Environment environment = environmentRepository.findById(value.environmentId());
        recordHistory(secret, system.name(), environment, HistoryAction.DELETED, value.value(), principal);
        configurationValueRepository.delete(value.id());
    }

    private void recordHistory(Configuration secret, String systemName, Environment environment, HistoryAction action,
                                EncryptedConfigurationValue valueSnapshot, AuthenticatedPrincipal principal) {
        ConfigurationValueHistoryEntry entry = ConfigurationValueHistoryEntry.recordChange(secret, systemName, environment, action, changeMetadataFor(principal));
        configurationValueHistoryRepository.record(entry, principal.userId(), valueSnapshot);
    }
}

