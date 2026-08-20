package com.alwaysmoveforward.configurationmanager.data.dao;

import com.alwaysmoveforward.configurationmanager.data.Entities.ConfigurationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConfigurationDAO extends JpaRepository<ConfigurationEntity, Long> {

    List<ConfigurationEntity> findBySystemIdOrderByNameAsc(Long systemId);

    Optional<ConfigurationEntity> findBySystemIdAndName(Long systemId, String name);
}

