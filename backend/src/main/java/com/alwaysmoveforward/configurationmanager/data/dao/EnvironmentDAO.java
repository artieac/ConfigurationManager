package com.alwaysmoveforward.configurationmanager.data.dao;

import com.alwaysmoveforward.configurationmanager.data.Entities.EnvironmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EnvironmentDAO extends JpaRepository<EnvironmentEntity, Long> {

    List<EnvironmentEntity> findBySystemIdOrderByNameAsc(Long systemId);

    Optional<EnvironmentEntity> findBySystemIdAndExternalId(Long systemId, String externalId);
}

