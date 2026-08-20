package com.alwaysmoveforward.configurationmanager.services;

import com.alwaysmoveforward.configurationmanager.data.repositories.EnvironmentRepository;
import com.alwaysmoveforward.configurationmanager.data.repositories.ConfigurationValueHistoryRepository;
import com.alwaysmoveforward.configurationmanager.data.repositories.ConfigurationRepository;
import com.alwaysmoveforward.configurationmanager.data.repositories.ConfigurationValueRepository;
import com.alwaysmoveforward.configurationmanager.data.repositories.SystemRepository;
import com.alwaysmoveforward.configurationmanager.domainmodel.*;
import com.alwaysmoveforward.configurationmanager.security.crypto.ConfigurationEncryptionService;
import com.alwaysmoveforward.configurationmanager.security.jwt.AuthenticatedPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ConfigurationValueServiceTest {

    private static final Long SYSTEM_ID = 1L;
    private static final Long SECRET_ID = 100L;
    private static final Long ENVIRONMENT_ID = 10L;
    private static final AuthenticatedPrincipal PRINCIPAL =
            new AuthenticatedPrincipal(9L, "admin@example.com", "Admin User", Role.ADMIN);

    private ConfigurationValueRepository configurationValueRepository;
    private ConfigurationValueHistoryRepository configurationValueHistoryRepository;
    private ConfigurationRepository configurationRepository;
    private SystemRepository systemRepository;
    private EnvironmentRepository environmentRepository;
    private ConfigurationEncryptionService encryptionService;
    private ConfigurationService configurationService;

    @BeforeEach
    void setUp() {
        configurationValueRepository = mock(ConfigurationValueRepository.class);
        configurationValueHistoryRepository = mock(ConfigurationValueHistoryRepository.class);
        configurationRepository = mock(ConfigurationRepository.class);
        systemRepository = mock(SystemRepository.class);
        environmentRepository = mock(EnvironmentRepository.class);
        encryptionService = mock(ConfigurationEncryptionService.class);
        configurationService = new ConfigurationService(configurationRepository, configurationValueRepository, configurationValueHistoryRepository,
                systemRepository, environmentRepository, encryptionService);

        Configuration secret = new Configuration(SECRET_ID, SYSTEM_ID, "db-password",
                new ChangeMetadata(1L, "Someone", Instant.now()), new ChangeMetadata(1L, "Someone", Instant.now()));
        when(configurationRepository.findById(SECRET_ID)).thenReturn(secret);

        ConfigurationSystem system = new ConfigurationSystem(SYSTEM_ID, "Payments API", "PaymentsAPI", "desc", new ChangeMetadata(1L, "Someone", Instant.now()));
        when(systemRepository.findById(SYSTEM_ID)).thenReturn(system);

        Environment environment = new Environment(ENVIRONMENT_ID, SYSTEM_ID, "Production", "Production",
                new ChangeMetadata(1L, "Someone", Instant.now()), new ChangeMetadata(1L, "Someone", Instant.now()));
        when(environmentRepository.findById(ENVIRONMENT_ID)).thenReturn(environment);
    }

    @Test
    void settingAValueForTheFirstTimeRecordsCreatedHistory() {
        when(configurationValueRepository.findByConfigurationIdAndEnvironmentId(SECRET_ID, ENVIRONMENT_ID)).thenReturn(Optional.empty());

        EncryptedConfigurationValue encrypted = new EncryptedConfigurationValue("cipher", "iv", 1);
        when(encryptionService.encrypt("hunter2")).thenReturn(encrypted);

        ConfigurationValue saved = new ConfigurationValue(500L, SECRET_ID, ENVIRONMENT_ID, encrypted,
                new ChangeMetadata(PRINCIPAL.userId(), PRINCIPAL.displayName(), Instant.now()));
        when(configurationValueRepository.create(any(), eq(PRINCIPAL.userId()))).thenReturn(saved);

        configurationService.setValue(SECRET_ID, ENVIRONMENT_ID, "hunter2", PRINCIPAL);

        ArgumentCaptor<ConfigurationValueHistoryEntry> captor = ArgumentCaptor.forClass(ConfigurationValueHistoryEntry.class);
        verify(configurationValueHistoryRepository).record(captor.capture(), eq(PRINCIPAL.userId()), eq(encrypted));
        assertEquals(HistoryAction.CREATED, captor.getValue().action());
        assertEquals("db-password", captor.getValue().configurationName());
        assertEquals("Production", captor.getValue().environmentName());
        verify(configurationValueRepository, never()).update(any());
    }

    @Test
    void settingAValueThatAlreadyExistsRecordsUpdatedHistory() {
        EncryptedConfigurationValue oldValue = new EncryptedConfigurationValue("old-cipher", "old-iv", 1);
        ConfigurationValue existing = new ConfigurationValue(500L, SECRET_ID, ENVIRONMENT_ID, oldValue, new ChangeMetadata(1L, "Creator", Instant.now()));
        when(configurationValueRepository.findByConfigurationIdAndEnvironmentId(SECRET_ID, ENVIRONMENT_ID)).thenReturn(Optional.of(existing));

        EncryptedConfigurationValue newValue = new EncryptedConfigurationValue("new-cipher", "new-iv", 1);
        when(encryptionService.encrypt("new-password")).thenReturn(newValue);
        when(configurationValueRepository.update(any())).thenReturn(existing.withUpdatedValue(newValue));

        configurationService.setValue(SECRET_ID, ENVIRONMENT_ID, "new-password", PRINCIPAL);

        ArgumentCaptor<ConfigurationValueHistoryEntry> captor = ArgumentCaptor.forClass(ConfigurationValueHistoryEntry.class);
        verify(configurationValueHistoryRepository).record(captor.capture(), eq(PRINCIPAL.userId()), eq(newValue));
        assertEquals(HistoryAction.UPDATED, captor.getValue().action());
        verify(configurationValueRepository, never()).create(any(), any());
    }

    @Test
    void revealAllForEnvironmentDecryptsEveryValueSetInThatEnvironment() {
        EncryptedConfigurationValue dbPasswordValue = new EncryptedConfigurationValue("db-cipher", "db-iv", 1);
        ConfigurationValue dbPasswordConfigurationValue = new ConfigurationValue(500L, SECRET_ID, ENVIRONMENT_ID, dbPasswordValue,
                new ChangeMetadata(1L, "Creator", Instant.now()));
        when(configurationValueRepository.findByEnvironmentIdWithConfiguration(ENVIRONMENT_ID)).thenReturn(List.of(new ConfigurationValueWithName(dbPasswordConfigurationValue, "db-password")));
        when(encryptionService.decrypt(dbPasswordValue)).thenReturn("hunter2");

        List<RevealedConfiguration> revealed = configurationService.revealAllForEnvironment(SYSTEM_ID, ENVIRONMENT_ID);

        assertEquals(1, revealed.size());
        assertEquals(SECRET_ID, revealed.get(0).configurationId());
        assertEquals("db-password", revealed.get(0).configurationName());
        assertEquals("hunter2", revealed.get(0).value());
    }

    @Test
    void revealAllForEnvironmentRejectsAnEnvironmentFromADifferentSystem() {
        Environment otherSystemsEnvironment = new Environment(ENVIRONMENT_ID, 999L, "Production", "Production",
                new ChangeMetadata(1L, "Someone", Instant.now()), new ChangeMetadata(1L, "Someone", Instant.now()));
        when(environmentRepository.findById(ENVIRONMENT_ID)).thenReturn(otherSystemsEnvironment);

        assertThrows(com.alwaysmoveforward.configurationmanager.exceptions.NotFoundException.class,
                () -> configurationService.revealAllForEnvironment(SYSTEM_ID, ENVIRONMENT_ID));
    }

    @Test
    void deletingAllValuesForAConfigurationRecordsDeletedHistoryForEachEnvironment() {
        EncryptedConfigurationValue value = new EncryptedConfigurationValue("cipher", "iv", 1);
        ConfigurationValue prodValue = new ConfigurationValue(500L, SECRET_ID, ENVIRONMENT_ID, value, new ChangeMetadata(1L, "Creator", Instant.now()));
        when(configurationValueRepository.findByConfigurationId(SECRET_ID)).thenReturn(List.of(prodValue));

        configurationService.deleteAllValuesForConfiguration(SECRET_ID, PRINCIPAL);

        ArgumentCaptor<ConfigurationValueHistoryEntry> captor = ArgumentCaptor.forClass(ConfigurationValueHistoryEntry.class);
        verify(configurationValueHistoryRepository).record(captor.capture(), eq(PRINCIPAL.userId()), eq(value));
        assertEquals(HistoryAction.DELETED, captor.getValue().action());
        verify(configurationValueRepository).delete(500L);
    }
}

