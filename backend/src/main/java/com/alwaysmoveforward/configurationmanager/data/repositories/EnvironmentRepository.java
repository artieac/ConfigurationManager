package com.alwaysmoveforward.configurationmanager.data.repositories;

import com.alwaysmoveforward.configurationmanager.data.Entities.EnvironmentEntity;
import com.alwaysmoveforward.configurationmanager.data.Entities.SystemEntity;
import com.alwaysmoveforward.configurationmanager.data.Entities.UserEntity;
import com.alwaysmoveforward.configurationmanager.data.dao.EnvironmentDAO;
import com.alwaysmoveforward.configurationmanager.data.dao.SystemDAO;
import com.alwaysmoveforward.configurationmanager.data.dao.UserDAO;
import com.alwaysmoveforward.configurationmanager.data.mapper.EnvironmentMapper;
import com.alwaysmoveforward.configurationmanager.domainmodel.Environment;
import com.alwaysmoveforward.configurationmanager.exceptions.ConflictException;
import com.alwaysmoveforward.configurationmanager.exceptions.NotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class EnvironmentRepository extends RepositoryBase {

    private final EnvironmentDAO environmentDAO;
    private final SystemDAO systemDAO;
    private final UserDAO userDAO;
    private final EnvironmentMapper environmentMapper;

    public EnvironmentRepository(EnvironmentDAO environmentDAO, SystemDAO systemDAO, UserDAO userDAO,
                                  EnvironmentMapper environmentMapper) {
        this.environmentDAO = environmentDAO;
        this.systemDAO = systemDAO;
        this.userDAO = userDAO;
        this.environmentMapper = environmentMapper;
    }

    public List<Environment> findBySystemId(Long systemId) {
        return environmentDAO.findBySystemIdOrderByNameAsc(systemId).stream().map(environmentMapper::toDomain).toList();
    }

    public Environment findById(Long id) {
        return orNotFound(environmentDAO.findById(id).map(environmentMapper::toDomain), () -> "Environment not found: " + id);
    }

    public Optional<Environment> findBySystemIdAndExternalId(Long systemId, String externalId) {
        return environmentDAO.findBySystemIdAndExternalId(systemId, externalId).map(environmentMapper::toDomain);
    }

    public Environment create(Environment environment, Long createdByUserId) {
        SystemEntity system = systemDAO.findById(environment.systemId())
                .orElseThrow(() -> new NotFoundException("System not found: " + environment.systemId()));
        UserEntity createdBy = userEntity(createdByUserId);
        try {
            EnvironmentEntity saved = environmentDAO.save(environmentMapper.toNewEntity(environment, system, createdBy));
            return environmentMapper.toDomain(saved);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("An environment named '" + environment.name() + "' or with external ID '"
                    + environment.externalId() + "' already exists in this system");
        }
    }

    public Environment update(Environment environment, Long updatedByUserId) {
        EnvironmentEntity existing = environmentDAO.findById(environment.id())
                .orElseThrow(() -> new NotFoundException("Environment not found: " + environment.id()));
        UserEntity updatedBy = userEntity(updatedByUserId);
        environmentMapper.applyToEntity(existing, environment, updatedBy);
        try {
            return environmentMapper.toDomain(environmentDAO.save(existing));
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("An environment named '" + environment.name() + "' or with external ID '"
                    + environment.externalId() + "' already exists in this system");
        }
    }

    /** Row-only delete — callers that need cascading value/history cleanup should go through EnvironmentService. */
    public void delete(Long id) {
        if (!environmentDAO.existsById(id)) {
            throw new NotFoundException("Environment not found: " + id);
        }
        environmentDAO.deleteById(id);
    }

    private UserEntity userEntity(Long userId) {
        return userDAO.findById(userId).orElseThrow(() -> new NotFoundException("User not found: " + userId));
    }
}

