package com.alwaysmoveforward.configurationmanager.data.mapper;

import com.alwaysmoveforward.configurationmanager.data.Entities.ConfigurationEntity;
import com.alwaysmoveforward.configurationmanager.data.Entities.SystemEntity;
import com.alwaysmoveforward.configurationmanager.data.Entities.UserEntity;
import com.alwaysmoveforward.configurationmanager.domainmodel.ChangeMetadata;
import com.alwaysmoveforward.configurationmanager.domainmodel.Configuration;
import org.springframework.stereotype.Component;

@Component
public class ConfigurationMapper {

    public Configuration toDomain(ConfigurationEntity entity) {
        return new Configuration(
                entity.getId(),
                entity.getSystem().getId(),
                entity.getName(),
                new ChangeMetadata(entity.getCreatedBy().getId(), entity.getCreatedBy().getDisplayName(), entity.getCreatedAt()),
                new ChangeMetadata(entity.getUpdatedBy().getId(), entity.getUpdatedBy().getDisplayName(), entity.getUpdatedAt()));
    }

    public ConfigurationEntity toNewEntity(Configuration secret, SystemEntity system, UserEntity createdBy) {
        return new ConfigurationEntity(system, secret.name(), createdBy, createdBy);
    }

    public void applyToEntity(ConfigurationEntity entity, Configuration secret, UserEntity updatedBy) {
        entity.setName(secret.name());
        entity.setUpdatedBy(updatedBy);
    }
}

