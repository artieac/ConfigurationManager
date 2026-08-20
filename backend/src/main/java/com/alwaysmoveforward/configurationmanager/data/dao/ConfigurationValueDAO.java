package com.alwaysmoveforward.configurationmanager.data.dao;

import com.alwaysmoveforward.configurationmanager.data.Entities.ConfigurationValueEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConfigurationValueDAO extends JpaRepository<ConfigurationValueEntity, Long> {

    List<ConfigurationValueEntity> findByConfigurationId(Long configurationId);

    @EntityGraph(attributePaths = {"configuration", "createdBy", "environment"})
    List<ConfigurationValueEntity> findWithConfigurationByEnvironmentId(Long environmentId);

    List<ConfigurationValueEntity> findByEnvironmentId(Long environmentId);

    Optional<ConfigurationValueEntity> findByConfigurationIdAndEnvironmentId(Long configurationId, Long environmentId);
}

