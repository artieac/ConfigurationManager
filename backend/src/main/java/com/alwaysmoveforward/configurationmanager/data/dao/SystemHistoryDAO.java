package com.alwaysmoveforward.configurationmanager.data.dao;

import com.alwaysmoveforward.configurationmanager.data.Entities.SystemHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SystemHistoryDAO extends JpaRepository<SystemHistoryEntity, Long> {

    List<SystemHistoryEntity> findBySystemIdOrderByChangedAtDesc(Long systemId);
}

