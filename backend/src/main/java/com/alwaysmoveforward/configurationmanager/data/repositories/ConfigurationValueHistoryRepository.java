package com.alwaysmoveforward.configurationmanager.data.repositories;

import com.alwaysmoveforward.configurationmanager.data.Entities.EnvironmentEntity;
import com.alwaysmoveforward.configurationmanager.data.Entities.ConfigurationEntity;
import com.alwaysmoveforward.configurationmanager.data.Entities.SystemEntity;
import com.alwaysmoveforward.configurationmanager.data.Entities.UserEntity;
import com.alwaysmoveforward.configurationmanager.data.dao.EnvironmentDAO;
import com.alwaysmoveforward.configurationmanager.data.dao.ConfigurationDAO;
import com.alwaysmoveforward.configurationmanager.data.dao.ConfigurationValueHistoryDAO;
import com.alwaysmoveforward.configurationmanager.data.dao.SystemDAO;
import com.alwaysmoveforward.configurationmanager.data.dao.UserDAO;
import com.alwaysmoveforward.configurationmanager.data.mapper.ConfigurationValueHistoryMapper;
import com.alwaysmoveforward.configurationmanager.domainmodel.EncryptedConfigurationValue;
import com.alwaysmoveforward.configurationmanager.domainmodel.ConfigurationValueHistoryEntry;
import com.alwaysmoveforward.configurationmanager.exceptions.NotFoundException;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class ConfigurationValueHistoryRepository extends RepositoryBase {

    private final ConfigurationValueHistoryDAO configurationValueHistoryDAO;
    private final ConfigurationDAO configurationDAO;
    private final SystemDAO systemDAO;
    private final EnvironmentDAO environmentDAO;
    private final UserDAO userDAO;
    private final ConfigurationValueHistoryMapper configurationValueHistoryMapper;

    public ConfigurationValueHistoryRepository(ConfigurationValueHistoryDAO configurationValueHistoryDAO, ConfigurationDAO configurationDAO, SystemDAO systemDAO,
                                         EnvironmentDAO environmentDAO, UserDAO userDAO,
                                         ConfigurationValueHistoryMapper configurationValueHistoryMapper) {
        this.configurationValueHistoryDAO = configurationValueHistoryDAO;
        this.configurationDAO = configurationDAO;
        this.systemDAO = systemDAO;
        this.environmentDAO = environmentDAO;
        this.userDAO = userDAO;
        this.configurationValueHistoryMapper = configurationValueHistoryMapper;
    }

    public List<ConfigurationValueHistoryEntry> findByConfigurationId(Long configurationId) {
        return configurationValueHistoryDAO.findByConfigurationIdOrderByChangedAtDesc(configurationId).stream()
                .map(configurationValueHistoryMapper::toDomain)
                .toList();
    }

    public List<ConfigurationValueHistoryEntry> findByConfigurationIdAndEnvironmentId(Long configurationId, Long environmentId) {
        return configurationValueHistoryDAO.findByConfigurationIdAndEnvironmentIdOrderByChangedAtDesc(configurationId, environmentId).stream()
                .map(configurationValueHistoryMapper::toDomain)
                .toList();
    }

    public List<ConfigurationValueHistoryEntry> findBySystemId(Long systemId) {
        return configurationValueHistoryDAO.findBySystemIdOrderByChangedAtDesc(systemId).stream()
                .map(configurationValueHistoryMapper::toDomain)
                .toList();
    }

    /**
     * The encrypted value as of this specific history entry — used only by the
     * explicit per-entry reveal endpoint, never returned alongside the plain
     * metadata list. {@code Optional.empty()} if the entry doesn't belong to
     * this secret or (defensively) never had a snapshot recorded.
     */
    public Optional<EncryptedConfigurationValue> findEncryptedSnapshot(Long configurationId, Long historyId) {
        return configurationValueHistoryDAO.findByConfigurationIdAndId(configurationId, historyId)
                .filter(entity -> entity.getEncryptedValueSnapshot() != null)
                .map(entity -> new EncryptedConfigurationValue(
                        entity.getEncryptedValueSnapshot(), entity.getEncryptionIvSnapshot(), entity.getKeyVersion()));
    }

    /**
     * Records one audit entry. {@code valueSnapshot} is optional forensic-only
     * ciphertext (never plaintext) — pass {@code null} when not needed.
     */
    public ConfigurationValueHistoryEntry record(ConfigurationValueHistoryEntry entry, Long changedByUserId, EncryptedConfigurationValue valueSnapshot) {
        // FK references are nullable by design: the referenced row may already be deleted before this history
        // entry is persisted (e.g. the secret/environment is removed before its last DELETED history row is written).
        // orElse(null) is intentional — history must survive the deletion of its parent objects.
        ConfigurationEntity secret = entry.configurationId() != null ? configurationDAO.findById(entry.configurationId()).orElse(null) : null;
        SystemEntity system = entry.systemId() != null ? systemDAO.findById(entry.systemId()).orElse(null) : null;
        EnvironmentEntity environment = entry.environmentId() != null
                ? environmentDAO.findById(entry.environmentId()).orElse(null)
                : null;
        UserEntity changedBy = userDAO.findById(changedByUserId)
                .orElseThrow(() -> new NotFoundException("User not found: " + changedByUserId));

        var entity = configurationValueHistoryMapper.toNewEntity(
                entry,
                secret,
                system,
                environment,
                changedBy,
                valueSnapshot != null ? valueSnapshot.ciphertextBase64() : null,
                valueSnapshot != null ? valueSnapshot.ivBase64() : null,
                valueSnapshot != null ? valueSnapshot.keyVersion() : null);

        return configurationValueHistoryMapper.toDomain(configurationValueHistoryDAO.save(entity));
    }
}

