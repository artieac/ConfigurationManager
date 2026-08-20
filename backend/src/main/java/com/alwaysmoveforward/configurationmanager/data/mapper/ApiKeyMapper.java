package com.alwaysmoveforward.configurationmanager.data.mapper;

import com.alwaysmoveforward.configurationmanager.data.Entities.ApiKeyEntity;
import com.alwaysmoveforward.configurationmanager.data.Entities.SystemEntity;
import com.alwaysmoveforward.configurationmanager.data.Entities.UserEntity;
import com.alwaysmoveforward.configurationmanager.domainmodel.ApiKey;
import com.alwaysmoveforward.configurationmanager.domainmodel.ChangeMetadata;
import org.springframework.stereotype.Component;

@Component
public class ApiKeyMapper {

    public ApiKey toDomain(ApiKeyEntity entity) {
        return new ApiKey(
                entity.getId(),
                entity.getSystem().getId(),
                entity.getName(),
                new ChangeMetadata(entity.getCreatedBy().getId(), entity.getCreatedBy().getDisplayName(), entity.getCreatedAt()),
                entity.getLastUsedAt());
    }

    public ApiKeyEntity toNewEntity(ApiKey apiKey, String tokenHash, SystemEntity system, UserEntity createdBy) {
        return new ApiKeyEntity(system, apiKey.name(), tokenHash, createdBy);
    }

    public void applyToEntity(ApiKeyEntity entity, ApiKey apiKey) {
        entity.setName(apiKey.name());
    }
}

