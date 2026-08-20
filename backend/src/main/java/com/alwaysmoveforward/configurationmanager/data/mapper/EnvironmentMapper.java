package com.alwaysmoveforward.configurationmanager.data.mapper;

import com.alwaysmoveforward.configurationmanager.data.Entities.EnvironmentEntity;
import com.alwaysmoveforward.configurationmanager.data.Entities.SystemEntity;
import com.alwaysmoveforward.configurationmanager.data.Entities.UserEntity;
import com.alwaysmoveforward.configurationmanager.domainmodel.ChangeMetadata;
import com.alwaysmoveforward.configurationmanager.domainmodel.Environment;
import org.springframework.stereotype.Component;

@Component
public class EnvironmentMapper {

    public Environment toDomain(EnvironmentEntity entity) {
        return new Environment(
                entity.getId(),
                entity.getSystem().getId(),
                entity.getName(),
                entity.getExternalId(),
                new ChangeMetadata(entity.getCreatedBy().getId(), entity.getCreatedBy().getDisplayName(), entity.getCreatedAt()),
                new ChangeMetadata(entity.getUpdatedBy().getId(), entity.getUpdatedBy().getDisplayName(), entity.getUpdatedAt()));
    }

    public EnvironmentEntity toNewEntity(Environment environment, SystemEntity system, UserEntity createdBy) {
        return new EnvironmentEntity(system, environment.name(), environment.externalId(), createdBy, createdBy);
    }

    public void applyToEntity(EnvironmentEntity entity, Environment environment, UserEntity updatedBy) {
        entity.setName(environment.name());
        entity.setExternalId(environment.externalId());
        entity.setUpdatedBy(updatedBy);
    }
}

