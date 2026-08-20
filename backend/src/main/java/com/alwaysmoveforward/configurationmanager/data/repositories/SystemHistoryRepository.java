package com.alwaysmoveforward.configurationmanager.data.repositories;

import com.alwaysmoveforward.configurationmanager.data.Entities.SystemEntity;
import com.alwaysmoveforward.configurationmanager.data.Entities.UserEntity;
import com.alwaysmoveforward.configurationmanager.data.dao.SystemDAO;
import com.alwaysmoveforward.configurationmanager.data.dao.SystemHistoryDAO;
import com.alwaysmoveforward.configurationmanager.data.dao.UserDAO;
import com.alwaysmoveforward.configurationmanager.data.mapper.SystemHistoryMapper;
import com.alwaysmoveforward.configurationmanager.domainmodel.SystemHistoryEntry;
import com.alwaysmoveforward.configurationmanager.exceptions.NotFoundException;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class SystemHistoryRepository extends RepositoryBase {

    private final SystemHistoryDAO systemHistoryDAO;
    private final SystemDAO systemDAO;
    private final UserDAO userDAO;
    private final SystemHistoryMapper systemHistoryMapper;

    public SystemHistoryRepository(SystemHistoryDAO systemHistoryDAO, SystemDAO systemDAO, UserDAO userDAO,
                                    SystemHistoryMapper systemHistoryMapper) {
        this.systemHistoryDAO = systemHistoryDAO;
        this.systemDAO = systemDAO;
        this.userDAO = userDAO;
        this.systemHistoryMapper = systemHistoryMapper;
    }

    public List<SystemHistoryEntry> findBySystemId(Long systemId) {
        return systemHistoryDAO.findBySystemIdOrderByChangedAtDesc(systemId).stream()
                .map(systemHistoryMapper::toDomain)
                .toList();
    }

    public SystemHistoryEntry record(SystemHistoryEntry entry, Long changedByUserId) {
        // FK reference is nullable by design: the system row may have been deleted before this history entry is persisted.
        // orElse(null) is intentional — history must survive the deletion of its parent system.
        SystemEntity system = entry.systemId() != null ? systemDAO.findById(entry.systemId()).orElse(null) : null;
        UserEntity changedBy = userDAO.findById(changedByUserId)
                .orElseThrow(() -> new NotFoundException("User not found: " + changedByUserId));

        var entity = systemHistoryMapper.toNewEntity(entry, system, changedBy);
        return systemHistoryMapper.toDomain(systemHistoryDAO.save(entity));
    }
}

