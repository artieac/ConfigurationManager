package com.alwaysmoveforward.configurationmanager.data.mapper;

import com.alwaysmoveforward.configurationmanager.data.Entities.SystemEntity;
import com.alwaysmoveforward.configurationmanager.data.Entities.SystemHistoryEntity;
import com.alwaysmoveforward.configurationmanager.data.Entities.UserEntity;
import com.alwaysmoveforward.configurationmanager.domainmodel.ChangeMetadata;
import com.alwaysmoveforward.configurationmanager.domainmodel.HistoryAction;
import com.alwaysmoveforward.configurationmanager.domainmodel.SystemHistoryEntry;
import org.springframework.stereotype.Component;

@Component
public class SystemHistoryMapper {

    public SystemHistoryEntry toDomain(SystemHistoryEntity entity) {
        return new SystemHistoryEntry(
                entity.getId(),
                entity.getSystem() != null ? entity.getSystem().getId() : null,
                entity.getSystemName(),
                entity.getExternalId(),
                entity.getDescription(),
                entity.getAction(),
                new ChangeMetadata(entity.getChangedBy().getId(), entity.getChangedBy().getDisplayName(), entity.getChangedAt()));
    }

    public SystemHistoryEntity toNewEntity(SystemHistoryEntry entry, SystemEntity system, UserEntity changedBy) {
        return new SystemHistoryEntity(
                system,
                entry.systemName(),
                entry.externalId(),
                entry.description(),
                entry.action(),
                changedBy);
    }
}

