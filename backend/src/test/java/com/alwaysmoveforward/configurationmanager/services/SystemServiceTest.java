package com.alwaysmoveforward.configurationmanager.services;

import com.alwaysmoveforward.configurationmanager.data.repositories.SystemHistoryRepository;
import com.alwaysmoveforward.configurationmanager.data.repositories.SystemRepository;
import com.alwaysmoveforward.configurationmanager.domainmodel.ChangeMetadata;
import com.alwaysmoveforward.configurationmanager.domainmodel.HistoryAction;
import com.alwaysmoveforward.configurationmanager.domainmodel.Role;
import com.alwaysmoveforward.configurationmanager.domainmodel.Configuration;
import com.alwaysmoveforward.configurationmanager.domainmodel.ConfigurationSystem;
import com.alwaysmoveforward.configurationmanager.domainmodel.SystemHistoryEntry;
import com.alwaysmoveforward.configurationmanager.security.jwt.AuthenticatedPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class SystemServiceTest {

    private static final Long SYSTEM_ID = 1L;
    private static final AuthenticatedPrincipal PRINCIPAL =
            new AuthenticatedPrincipal(9L, "admin@example.com", "Admin User", Role.ADMIN);

    private SystemRepository systemRepository;
    private SystemHistoryRepository systemHistoryRepository;
    private ConfigurationService configurationService;
    private SystemService systemService;

    @BeforeEach
    void setUp() {
        systemRepository = mock(SystemRepository.class);
        systemHistoryRepository = mock(SystemHistoryRepository.class);
        configurationService = mock(ConfigurationService.class);
        systemService = new SystemService(systemRepository, systemHistoryRepository, configurationService);
    }

    @Test
    void createSystemRecordsCreatedHistory() {
        ConfigurationSystem saved = new ConfigurationSystem(SYSTEM_ID, "Payments API", "PaymentsAPI", "desc",
                new ChangeMetadata(PRINCIPAL.userId(), PRINCIPAL.displayName(), Instant.now()));
        when(systemRepository.create(any(), eq(PRINCIPAL.userId()))).thenReturn(saved);

        systemService.createSystem("Payments API", "PaymentsAPI", "desc", PRINCIPAL);

        ArgumentCaptor<SystemHistoryEntry> captor = ArgumentCaptor.forClass(SystemHistoryEntry.class);
        verify(systemHistoryRepository).record(captor.capture(), eq(PRINCIPAL.userId()));
        assertEquals(HistoryAction.CREATED, captor.getValue().action());
        assertEquals("Payments API", captor.getValue().systemName());
    }

    @Test
    void updateSystemRecordsUpdatedHistory() {
        ConfigurationSystem existing = new ConfigurationSystem(SYSTEM_ID, "Payments API", "PaymentsAPI", "desc",
                new ChangeMetadata(1L, "Creator", Instant.now()));
        when(systemRepository.findById(SYSTEM_ID)).thenReturn(existing);

        ConfigurationSystem updated = existing.withUpdatedDetails("Payments API v2", "PaymentsAPIv2", "new desc");
        when(systemRepository.update(any())).thenReturn(updated);

        systemService.updateSystem(SYSTEM_ID, "Payments API v2", "PaymentsAPIv2", "new desc", PRINCIPAL);

        ArgumentCaptor<SystemHistoryEntry> captor = ArgumentCaptor.forClass(SystemHistoryEntry.class);
        verify(systemHistoryRepository).record(captor.capture(), eq(PRINCIPAL.userId()));
        assertEquals(HistoryAction.UPDATED, captor.getValue().action());
        assertEquals("Payments API v2", captor.getValue().systemName());
    }

    @Test
    void deleteSystemCascadesThroughConfigurationsThenRecordsDeletedHistoryThenDeletesTheRow() {
        ConfigurationSystem existing = new ConfigurationSystem(SYSTEM_ID, "Payments API", "PaymentsAPI", "desc",
                new ChangeMetadata(1L, "Creator", Instant.now()));
        when(systemRepository.findById(SYSTEM_ID)).thenReturn(existing);

        Configuration secret = new Configuration(50L, SYSTEM_ID, "db-password",
                new ChangeMetadata(1L, "Creator", Instant.now()), new ChangeMetadata(1L, "Creator", Instant.now()));
        when(configurationService.listConfigurations(SYSTEM_ID)).thenReturn(List.of(secret));

        systemService.deleteSystem(SYSTEM_ID, PRINCIPAL);

        var inOrder = inOrder(configurationService, systemHistoryRepository, systemRepository);
        inOrder.verify(configurationService).deleteConfiguration(50L, PRINCIPAL);
        inOrder.verify(systemHistoryRepository).record(any(), eq(PRINCIPAL.userId()));
        inOrder.verify(systemRepository).delete(SYSTEM_ID);
    }
}

