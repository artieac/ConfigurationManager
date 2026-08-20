package com.alwaysmoveforward.configurationmanager.data.dao;

import com.alwaysmoveforward.configurationmanager.data.Entities.ApiKeyEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ApiKeyDAO extends JpaRepository<ApiKeyEntity, Long> {

    List<ApiKeyEntity> findBySystemIdOrderByNameAsc(Long systemId);

    Optional<ApiKeyEntity> findByTokenHash(String tokenHash);
}

