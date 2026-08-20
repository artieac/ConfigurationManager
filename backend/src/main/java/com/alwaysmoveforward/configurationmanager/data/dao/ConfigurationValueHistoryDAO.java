package com.alwaysmoveforward.configurationmanager.data.dao;

import com.alwaysmoveforward.configurationmanager.data.Entities.ConfigurationValueHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConfigurationValueHistoryDAO extends JpaRepository<ConfigurationValueHistoryEntity, Long> {

    List<ConfigurationValueHistoryEntity> findByConfigurationIdOrderByChangedAtDesc(Long configurationId);

    List<ConfigurationValueHistoryEntity> findByConfigurationIdAndEnvironmentIdOrderByChangedAtDesc(Long configurationId, Long environmentId);

    List<ConfigurationValueHistoryEntity> findBySystemIdOrderByChangedAtDesc(Long systemId);

    /** Scoped by configurationId so a caller can't probe another secret's history by guessing ids. */
    Optional<ConfigurationValueHistoryEntity> findByConfigurationIdAndId(Long configurationId, Long id);
}

