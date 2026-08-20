package com.alwaysmoveforward.configurationmanager.data.repositories;

import com.alwaysmoveforward.configurationmanager.data.Entities.SystemEntity;
import com.alwaysmoveforward.configurationmanager.data.Entities.UserEntity;
import com.alwaysmoveforward.configurationmanager.data.dao.SystemDAO;
import com.alwaysmoveforward.configurationmanager.data.dao.UserDAO;
import com.alwaysmoveforward.configurationmanager.data.mapper.SystemMapper;
import com.alwaysmoveforward.configurationmanager.domainmodel.ConfigurationSystem;
import com.alwaysmoveforward.configurationmanager.exceptions.ConflictException;
import com.alwaysmoveforward.configurationmanager.exceptions.NotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class SystemRepository extends RepositoryBase {

    private final SystemDAO systemDAO;
    private final UserDAO userDAO;
    private final SystemMapper systemMapper;

    public SystemRepository(SystemDAO systemDAO, UserDAO userDAO, SystemMapper systemMapper) {
        this.systemDAO = systemDAO;
        this.userDAO = userDAO;
        this.systemMapper = systemMapper;
    }

    public List<ConfigurationSystem> findAll() {
        return systemDAO.findAll().stream().map(systemMapper::toDomain).toList();
    }

    public ConfigurationSystem findById(Long id) {
        return orNotFound(systemDAO.findById(id).map(systemMapper::toDomain), () -> "System not found: " + id);
    }

    public Optional<ConfigurationSystem> findByExternalId(String externalId) {
        return systemDAO.findByExternalId(externalId).map(systemMapper::toDomain);
    }

    public ConfigurationSystem create(ConfigurationSystem system, Long createdByUserId) {
        UserEntity createdBy = userEntity(createdByUserId);
        try {
            SystemEntity saved = systemDAO.save(systemMapper.toNewEntity(system, createdBy));
            return systemMapper.toDomain(saved);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("A system named '" + system.name() + "' or with external ID '" + system.externalId() + "' already exists");
        }
    }

    public ConfigurationSystem update(ConfigurationSystem system) {
        SystemEntity existing = systemDAO.findById(system.id())
                .orElseThrow(() -> new NotFoundException("System not found: " + system.id()));
        systemMapper.applyToEntity(existing, system);
        try {
            return systemMapper.toDomain(systemDAO.save(existing));
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("A system named '" + system.name() + "' or with external ID '" + system.externalId() + "' already exists");
        }
    }

    public void delete(Long id) {
        if (!systemDAO.existsById(id)) {
            throw new NotFoundException("System not found: " + id);
        }
        systemDAO.deleteById(id);
    }

    private UserEntity userEntity(Long userId) {
        return userDAO.findById(userId).orElseThrow(() -> new NotFoundException("User not found: " + userId));
    }
}

