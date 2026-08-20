package com.alwaysmoveforward.configurationmanager.data.repositories;

import com.alwaysmoveforward.configurationmanager.data.Entities.ConfigurationEntity;
import com.alwaysmoveforward.configurationmanager.data.Entities.SystemEntity;
import com.alwaysmoveforward.configurationmanager.data.Entities.UserEntity;
import com.alwaysmoveforward.configurationmanager.data.dao.ConfigurationDAO;
import com.alwaysmoveforward.configurationmanager.data.dao.SystemDAO;
import com.alwaysmoveforward.configurationmanager.data.dao.UserDAO;
import com.alwaysmoveforward.configurationmanager.data.mapper.ConfigurationMapper;
import com.alwaysmoveforward.configurationmanager.domainmodel.Configuration;
import com.alwaysmoveforward.configurationmanager.exceptions.ConflictException;
import com.alwaysmoveforward.configurationmanager.exceptions.NotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class ConfigurationRepository extends RepositoryBase {

    private final ConfigurationDAO configurationDAO;
    private final SystemDAO systemDAO;
    private final UserDAO userDAO;
    private final ConfigurationMapper secretMapper;

    public ConfigurationRepository(ConfigurationDAO configurationDAO, SystemDAO systemDAO, UserDAO userDAO, ConfigurationMapper secretMapper) {
        this.configurationDAO = configurationDAO;
        this.systemDAO = systemDAO;
        this.userDAO = userDAO;
        this.secretMapper = secretMapper;
    }

    public List<Configuration> findBySystemId(Long systemId) {
        return configurationDAO.findBySystemIdOrderByNameAsc(systemId).stream().map(secretMapper::toDomain).toList();
    }

    public Configuration findById(Long id) {
        return orNotFound(configurationDAO.findById(id).map(secretMapper::toDomain), () -> "Configuration not found: " + id);
    }

    public Optional<Configuration> findBySystemIdAndName(Long systemId, String name) {
        return configurationDAO.findBySystemIdAndName(systemId, name).map(secretMapper::toDomain);
    }

    public Configuration create(Configuration secret, Long createdByUserId) {
        SystemEntity system = systemDAO.findById(secret.systemId())
                .orElseThrow(() -> new NotFoundException("System not found: " + secret.systemId()));
        UserEntity createdBy = userEntity(createdByUserId);
        try {
            ConfigurationEntity saved = configurationDAO.save(secretMapper.toNewEntity(secret, system, createdBy));
            return secretMapper.toDomain(saved);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("A secret named '" + secret.name() + "' already exists in this system");
        }
    }

    public Configuration update(Configuration secret, Long updatedByUserId) {
        ConfigurationEntity existing = configurationDAO.findById(secret.id())
                .orElseThrow(() -> new NotFoundException("Configuration not found: " + secret.id()));
        UserEntity updatedBy = userEntity(updatedByUserId);
        secretMapper.applyToEntity(existing, secret, updatedBy);
        try {
            return secretMapper.toDomain(configurationDAO.save(existing));
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("A secret named '" + secret.name() + "' already exists in this system");
        }
    }

    /** Row-only delete — callers that need cascading value/history cleanup should go through ConfigurationService. */
    public void delete(Long id) {
        if (!configurationDAO.existsById(id)) {
            throw new NotFoundException("Configuration not found: " + id);
        }
        configurationDAO.deleteById(id);
    }

    private UserEntity userEntity(Long userId) {
        return userDAO.findById(userId).orElseThrow(() -> new NotFoundException("User not found: " + userId));
    }
}

