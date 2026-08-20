package com.alwaysmoveforward.configurationmanager.data.dao;

import com.alwaysmoveforward.configurationmanager.data.Entities.SystemEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SystemDAO extends JpaRepository<SystemEntity, Long> {

    Optional<SystemEntity> findByExternalId(String externalId);
}

