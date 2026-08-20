package com.alwaysmoveforward.configurationmanager.data.mapper;

import com.alwaysmoveforward.configurationmanager.data.Entities.SystemEntity;
import com.alwaysmoveforward.configurationmanager.data.Entities.UserEntity;
import com.alwaysmoveforward.configurationmanager.domainmodel.ChangeMetadata;
import com.alwaysmoveforward.configurationmanager.domainmodel.ConfigurationSystem;
import org.springframework.stereotype.Component;

@Component
public class SystemMapper {

    public ConfigurationSystem toDomain(SystemEntity entity) {
        return new ConfigurationSystem(
                entity.getId(),
                entity.getName(),
                entity.getExternalId(),
                entity.getDescription(),
                new ChangeMetadata(entity.getCreatedBy().getId(), entity.getCreatedBy().getDisplayName(), entity.getCreatedAt()));
    }

    public SystemEntity toNewEntity(ConfigurationSystem system, UserEntity createdBy) {
        return new SystemEntity(system.name(), system.externalId(), system.description(), createdBy);
    }

    public void applyToEntity(SystemEntity entity, ConfigurationSystem system) {
        entity.setName(system.name());
        entity.setExternalId(system.externalId());
        entity.setDescription(system.description());
    }
}

