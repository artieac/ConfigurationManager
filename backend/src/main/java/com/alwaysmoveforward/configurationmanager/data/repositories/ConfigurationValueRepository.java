package com.alwaysmoveforward.configurationmanager.data.repositories;

import com.alwaysmoveforward.configurationmanager.data.Entities.EnvironmentEntity;
import com.alwaysmoveforward.configurationmanager.data.Entities.ConfigurationEntity;
import com.alwaysmoveforward.configurationmanager.data.Entities.ConfigurationValueEntity;
import com.alwaysmoveforward.configurationmanager.data.Entities.UserEntity;
import com.alwaysmoveforward.configurationmanager.data.dao.EnvironmentDAO;
import com.alwaysmoveforward.configurationmanager.data.dao.ConfigurationDAO;
import com.alwaysmoveforward.configurationmanager.data.dao.ConfigurationValueDAO;
import com.alwaysmoveforward.configurationmanager.data.dao.UserDAO;
import com.alwaysmoveforward.configurationmanager.data.mapper.ConfigurationValueMapper;
import com.alwaysmoveforward.configurationmanager.domainmodel.ConfigurationValue;
import com.alwaysmoveforward.configurationmanager.domainmodel.ConfigurationValueWithName;
import com.alwaysmoveforward.configurationmanager.exceptions.NotFoundException;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class ConfigurationValueRepository extends RepositoryBase {

    private final ConfigurationValueDAO configurationValueDAO;
    private final ConfigurationDAO configurationDAO;
    private final EnvironmentDAO environmentDAO;
    private final UserDAO userDAO;
    private final ConfigurationValueMapper configurationValueMapper;

    public ConfigurationValueRepository(ConfigurationValueDAO configurationValueDAO, ConfigurationDAO configurationDAO, EnvironmentDAO environmentDAO,
                                  UserDAO userDAO, ConfigurationValueMapper configurationValueMapper) {
        this.configurationValueDAO = configurationValueDAO;
        this.configurationDAO = configurationDAO;
        this.environmentDAO = environmentDAO;
        this.userDAO = userDAO;
        this.configurationValueMapper = configurationValueMapper;
    }

    public List<ConfigurationValue> findByConfigurationId(Long configurationId) {
        return configurationValueDAO.findByConfigurationId(configurationId).stream().map(configurationValueMapper::toDomain).toList();
    }

    public Optional<ConfigurationValue> findByConfigurationIdAndEnvironmentId(Long configurationId, Long environmentId) {
        return configurationValueDAO.findByConfigurationIdAndEnvironmentId(configurationId, environmentId).map(configurationValueMapper::toDomain);
    }

    public List<ConfigurationValue> findByEnvironmentId(Long environmentId) {
        return configurationValueDAO.findByEnvironmentId(environmentId).stream().map(configurationValueMapper::toDomain).toList();
    }

    /** Fetches values for an environment with the associated Configuration pre-loaded via a JOIN — use this on the bulk-reveal
     *  path to avoid N+1 queries when the caller needs both the secret name and the encrypted value. */
    public List<ConfigurationValueWithName> findByEnvironmentIdWithConfiguration(Long environmentId) {
        return configurationValueDAO.findWithConfigurationByEnvironmentId(environmentId).stream()
                .map(entity -> new ConfigurationValueWithName(configurationValueMapper.toDomain(entity), entity.getConfiguration().getName()))
                .toList();
    }

    public ConfigurationValue create(ConfigurationValue configurationValue, Long createdByUserId) {
        ConfigurationEntity secret = configurationDAO.findById(configurationValue.configurationId())
                .orElseThrow(() -> new NotFoundException("Configuration not found: " + configurationValue.configurationId()));
        EnvironmentEntity environment = environmentDAO.findById(configurationValue.environmentId())
                .orElseThrow(() -> new NotFoundException("Environment not found: " + configurationValue.environmentId()));
        UserEntity createdBy = userEntity(createdByUserId);

        ConfigurationValueEntity saved = configurationValueDAO.save(
                configurationValueMapper.toNewEntity(configurationValue, secret, environment, createdBy));
        return configurationValueMapper.toDomain(saved);
    }

    public ConfigurationValue update(ConfigurationValue configurationValue) {
        ConfigurationValueEntity existing = configurationValueDAO.findById(configurationValue.id())
                .orElseThrow(() -> new NotFoundException("Configuration value not found: " + configurationValue.id()));
        configurationValueMapper.applyToEntity(existing, configurationValue);
        return configurationValueMapper.toDomain(configurationValueDAO.save(existing));
    }

    public void delete(Long id) {
        if (!configurationValueDAO.existsById(id)) {
            throw new NotFoundException("Configuration value not found: " + id);
        }
        configurationValueDAO.deleteById(id);
    }

    private UserEntity userEntity(Long userId) {
        return userDAO.findById(userId).orElseThrow(() -> new NotFoundException("User not found: " + userId));
    }
}

