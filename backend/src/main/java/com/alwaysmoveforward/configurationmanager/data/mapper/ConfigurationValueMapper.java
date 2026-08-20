package com.alwaysmoveforward.configurationmanager.data.mapper;

import com.alwaysmoveforward.configurationmanager.data.Entities.EnvironmentEntity;
import com.alwaysmoveforward.configurationmanager.data.Entities.ConfigurationEntity;
import com.alwaysmoveforward.configurationmanager.data.Entities.ConfigurationValueEntity;
import com.alwaysmoveforward.configurationmanager.data.Entities.UserEntity;
import com.alwaysmoveforward.configurationmanager.domainmodel.ChangeMetadata;
import com.alwaysmoveforward.configurationmanager.domainmodel.EncryptedConfigurationValue;
import com.alwaysmoveforward.configurationmanager.domainmodel.ConfigurationValue;
import org.springframework.stereotype.Component;

@Component
public class ConfigurationValueMapper {

    public ConfigurationValue toDomain(ConfigurationValueEntity entity) {
        return new ConfigurationValue(
                entity.getId(),
                entity.getConfiguration().getId(),
                entity.getEnvironment().getId(),
                new EncryptedConfigurationValue(entity.getEncryptedValue(), entity.getEncryptionIv(), entity.getKeyVersion()),
                new ChangeMetadata(entity.getCreatedBy().getId(), entity.getCreatedBy().getDisplayName(), entity.getCreatedAt()));
    }

    public ConfigurationValueEntity toNewEntity(ConfigurationValue configurationValue, ConfigurationEntity secret, EnvironmentEntity environment,
                                          UserEntity createdBy) {
        EncryptedConfigurationValue value = configurationValue.value();
        return new ConfigurationValueEntity(secret, environment, value.ciphertextBase64(), value.ivBase64(),
                value.keyVersion(), createdBy);
    }

    public void applyToEntity(ConfigurationValueEntity entity, ConfigurationValue configurationValue) {
        EncryptedConfigurationValue value = configurationValue.value();
        entity.setEncryptedValue(value.ciphertextBase64());
        entity.setEncryptionIv(value.ivBase64());
        entity.setKeyVersion(value.keyVersion());
    }
}

