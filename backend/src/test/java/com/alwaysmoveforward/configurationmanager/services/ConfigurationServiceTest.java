package com.alwaysmoveforward.configurationmanager.services;

import com.alwaysmoveforward.configurationmanager.data.repositories.ConfigurationRepository;
import com.alwaysmoveforward.configurationmanager.domainmodel.ChangeMetadata;
import com.alwaysmoveforward.configurationmanager.domainmodel.Role;
import com.alwaysmoveforward.configurationmanager.domainmodel.Configuration;
import com.alwaysmoveforward.configurationmanager.security.jwt.AuthenticatedPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ConfigurationServiceTest {

    private static final Long SYSTEM_ID = 1L;
    private static final Long SECRET_ID = 100L;
    private static final AuthenticatedPrincipal PRINCIPAL =
            new AuthenticatedPrincipal(9L, "admin@example.com", "Admin User", Role.ADMIN);

    private ConfigurationRepository configurationRepository;
    private com.alwaysmoveforward.configurationmanager.data.repositories.ConfigurationValueRepository configurationValueRepository;
    private com.alwaysmoveforward.configurationmanager.data.repositories.ConfigurationValueHistoryRepository configurationValueHistoryRepository;
    private com.alwaysmoveforward.configurationmanager.data.repositories.SystemRepository systemRepository;
    private com.alwaysmoveforward.configurationmanager.data.repositories.EnvironmentRepository environmentRepository;
    private com.alwaysmoveforward.configurationmanager.services.ConfigurationEncryptor encryptionService;
    private ConfigurationService configurationService;

    @BeforeEach
    void setUp() {
        configurationRepository = mock(ConfigurationRepository.class);
        configurationValueRepository = mock(com.alwaysmoveforward.configurationmanager.data.repositories.ConfigurationValueRepository.class);
        configurationValueHistoryRepository = mock(com.alwaysmoveforward.configurationmanager.data.repositories.ConfigurationValueHistoryRepository.class);
        systemRepository = mock(com.alwaysmoveforward.configurationmanager.data.repositories.SystemRepository.class);
        environmentRepository = mock(com.alwaysmoveforward.configurationmanager.data.repositories.EnvironmentRepository.class);
        encryptionService = mock(com.alwaysmoveforward.configurationmanager.services.ConfigurationEncryptor.class);
        
        configurationService = new ConfigurationService(
                configurationRepository, 
                configurationValueRepository, 
                configurationValueHistoryRepository,
                systemRepository, 
                environmentRepository, 
                encryptionService);
    }

    @Test
    void createConfigurationPersistsANameOnlyRow() {
        Configuration saved = new Configuration(SECRET_ID, SYSTEM_ID, "db-password",
                new ChangeMetadata(PRINCIPAL.userId(), PRINCIPAL.displayName(), Instant.now()),
                new ChangeMetadata(PRINCIPAL.userId(), PRINCIPAL.displayName(), Instant.now()));
        when(configurationRepository.create(any(), eq(PRINCIPAL.userId()))).thenReturn(saved);

        Configuration result = configurationService.createConfiguration(SYSTEM_ID, "db-password", PRINCIPAL);

        assertEquals("db-password", result.name());
        verifyNoInteractions(configurationValueRepository);
    }

    @Test
    void deleteConfigurationCascadesThroughValuesBeforeDeletingTheRow() {
        Configuration secret = new Configuration(SECRET_ID, SYSTEM_ID, "db-password",
                new ChangeMetadata(1L, "Someone", Instant.now()), null);
        when(configurationRepository.findById(SECRET_ID)).thenReturn(secret);
        
        com.alwaysmoveforward.configurationmanager.domainmodel.ConfigurationSystem system = new com.alwaysmoveforward.configurationmanager.domainmodel.ConfigurationSystem(SYSTEM_ID, "sys", "sys", "desc", new ChangeMetadata(1L, "Someone", Instant.now()));
        when(systemRepository.findById(SYSTEM_ID)).thenReturn(system);
        
        when(configurationValueRepository.findByConfigurationId(SECRET_ID)).thenReturn(java.util.Collections.emptyList());

        configurationService.deleteConfiguration(SECRET_ID, PRINCIPAL);

        var inOrder = inOrder(configurationValueRepository, configurationRepository);
        inOrder.verify(configurationValueRepository).findByConfigurationId(SECRET_ID);
        inOrder.verify(configurationRepository).delete(SECRET_ID);
    }
}

