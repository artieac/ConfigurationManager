package com.alwaysmoveforward.configurationmanager.data.mapper;

import com.alwaysmoveforward.configurationmanager.data.Entities.EnvironmentEntity;
import com.alwaysmoveforward.configurationmanager.data.Entities.ConfigurationEntity;
import com.alwaysmoveforward.configurationmanager.data.Entities.ConfigurationValueHistoryEntity;
import com.alwaysmoveforward.configurationmanager.data.Entities.SystemEntity;
import com.alwaysmoveforward.configurationmanager.data.Entities.UserEntity;
import com.alwaysmoveforward.configurationmanager.domainmodel.ChangeMetadata;
import com.alwaysmoveforward.configurationmanager.domainmodel.HistoryAction;
import com.alwaysmoveforward.configurationmanager.domainmodel.ConfigurationValueHistoryEntry;
import org.springframework.stereotype.Component;

@Component
public class ConfigurationValueHistoryMapper {

    public ConfigurationValueHistoryEntry toDomain(ConfigurationValueHistoryEntity entity) {
        return new ConfigurationValueHistoryEntry(
                entity.getId(),
                entity.getConfiguration() != null ? entity.getConfiguration().getId() : null,
                entity.getSystem() != null ? entity.getSystem().getId() : null,
                entity.getEnvironment() != null ? entity.getEnvironment().getId() : null,
                entity.getConfigurationName(),
                entity.getSystemName(),
                entity.getEnvironmentName(),
                entity.getAction(),
                new ChangeMetadata(entity.getChangedBy().getId(), entity.getChangedBy().getDisplayName(), entity.getChangedAt()));
    }

    /**
     * Builds the history row. The encrypted snapshot fields are forensic-only —
     * no domain model or view model surfaces them back out (see package docs).
     */
    public ConfigurationValueHistoryEntity toNewEntity(ConfigurationValueHistoryEntry entry, ConfigurationEntity secret, SystemEntity system,
                                                 EnvironmentEntity environment, UserEntity changedBy,
                                                 String encryptedValueSnapshot, String encryptionIvSnapshot, Integer keyVersion) {
        return new ConfigurationValueHistoryEntity(
                secret,
                system,
                environment,
                entry.configurationName(),
                entry.systemName(),
                entry.environmentName(),
                entry.action(),
                encryptedValueSnapshot,
                encryptionIvSnapshot,
                keyVersion,
                changedBy);
    }
}

