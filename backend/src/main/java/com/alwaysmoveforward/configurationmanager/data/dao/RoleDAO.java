package com.alwaysmoveforward.configurationmanager.data.dao;

import com.alwaysmoveforward.configurationmanager.data.Entities.RoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleDAO extends JpaRepository<RoleEntity, Long> {

    Optional<RoleEntity> findByName(String name);
}

