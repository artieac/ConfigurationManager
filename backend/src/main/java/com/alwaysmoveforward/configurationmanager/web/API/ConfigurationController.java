package com.alwaysmoveforward.configurationmanager.web.API;

import com.alwaysmoveforward.configurationmanager.security.apikey.ApiClientPrincipal;
import com.alwaysmoveforward.configurationmanager.security.jwt.AuthenticatedPrincipal;
import com.alwaysmoveforward.configurationmanager.services.EnvironmentService;
import com.alwaysmoveforward.configurationmanager.services.ConfigurationService;
import com.alwaysmoveforward.configurationmanager.services.SystemService;
import com.alwaysmoveforward.configurationmanager.web.Models.HistoricConfigurationValueViewModel;
import com.alwaysmoveforward.configurationmanager.web.Models.ConfigurationValueHistoryViewModel;
import com.alwaysmoveforward.configurationmanager.web.Models.ConfigurationRequest;
import com.alwaysmoveforward.configurationmanager.web.Models.ConfigurationValueRequest;
import com.alwaysmoveforward.configurationmanager.web.Models.ConfigurationValueViewModel;
import com.alwaysmoveforward.configurationmanager.web.Models.ConfigurationViewModel;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class ConfigurationController extends ControllerBase {

    private final ConfigurationService configurationService;
    private final EnvironmentService environmentService;
    private final SystemService systemService;

    public ConfigurationController(ConfigurationService configurationService, EnvironmentService environmentService, SystemService systemService) {
        this.configurationService = configurationService;
        this.environmentService = environmentService;
        this.systemService = systemService;
    }

    @GetMapping("/api/systems/{systemId}/secrets")
    @PreAuthorize("hasRole('READ_ONLY')")
    public List<ConfigurationViewModel> listConfigurations(@PathVariable Long systemId) {
        return configurationService.listConfigurations(systemId).stream()
                .map(secret -> ConfigurationViewModel.from(secret, configurationService.listValues(secret.id())))
                .toList();
    }

    @PostMapping("/api/systems/{systemId}/secrets")
    @PreAuthorize("hasRole('READ_WRITE')")
    public ResponseEntity<ConfigurationViewModel> createConfiguration(@PathVariable Long systemId,
                                                          @Valid @RequestBody ConfigurationRequest request,
                                                          @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        var created = configurationService.createConfiguration(systemId, request.name(), principal);
        return ResponseEntity.status(201).body(ConfigurationViewModel.from(created, List.of()));
    }

    @GetMapping("/api/secrets/{id}")
    @PreAuthorize("hasRole('READ_ONLY')")
    public ConfigurationViewModel getConfiguration(@PathVariable Long id) {
        var secret = configurationService.getConfiguration(id);
        return ConfigurationViewModel.from(secret, configurationService.listValues(id));
    }

    @PutMapping("/api/secrets/{id}")
    @PreAuthorize("hasRole('READ_WRITE')")
    public ConfigurationViewModel renameConfiguration(@PathVariable Long id, @Valid @RequestBody ConfigurationRequest request,
                                         @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        var renamed = configurationService.renameConfiguration(id, request.name(), principal);
        return ConfigurationViewModel.from(renamed, configurationService.listValues(id));
    }

    /** Deletes the secret and its value in every environment (each generating its own DELETED history entry). */
    @DeleteMapping("/api/secrets/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteConfiguration(@PathVariable Long id, @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        configurationService.deleteConfiguration(id, principal);
        return ResponseEntity.noContent().build();
    }

    /** The only endpoint in the system that returns a decrypted value — restricted to READ_WRITE and ADMIN. */
    @GetMapping("/api/secrets/{id}/environments/{environmentId}/value")
    @PreAuthorize("hasRole('READ_WRITE')")
    public ConfigurationValueViewModel revealValue(@PathVariable Long id, @PathVariable Long environmentId) {
        var secret = configurationService.getConfiguration(id);
        var environment = environmentService.getEnvironment(environmentId);
        String value = configurationService.revealValue(id, environmentId);
        return new ConfigurationValueViewModel(secret.id(), environmentId, secret.name(), environment.name(), value);
    }

    /**
     * Bulk reveal for machine clients — every secret's decrypted value currently set in this
     * environment (e.g. to pull a whole environment's config for a deploy). Requires an API key
     * (see ApiKeyController), not a user session. Addressed by system/environment EXTERNAL ID —
     * a separate, URL-safe identifier from the internal numeric id and independent of the
     * human-facing `name` (see SystemRequest/EnvironmentRequest), so renaming a system/environment
     * never breaks a client already configured with its external id. A key only ever grants access
     * to the one system it was issued for, checked explicitly below since @PreAuthorize can't
     * express that.
     */
    @GetMapping("/api/systems/{systemExternalId}/environments/{environmentExternalId}/values")
    @PreAuthorize("hasRole('API_CLIENT')")
    public List<ConfigurationValueViewModel> revealConfigurationsForEnvironment(@PathVariable String systemExternalId, @PathVariable String environmentExternalId,
                                                                    @AuthenticationPrincipal ApiClientPrincipal apiClientPrincipal) {
        var system = systemService.getSystemByExternalId(systemExternalId);
        if (!apiClientPrincipal.systemId().equals(system.id())) {
            throw new AccessDeniedException("This API key is not valid for system '" + systemExternalId + "'");
        }

        var environment = environmentService.getEnvironmentByExternalId(system.id(), environmentExternalId);
        return configurationService.revealAllForEnvironment(system.id(), environment.id()).stream()
                .map(revealed -> new ConfigurationValueViewModel(revealed.configurationId(), environment.id(), revealed.configurationName(), environment.name(), revealed.value()))
                .toList();
    }

    /** Upsert: sets the value for this secret in this environment, whether or not one was already set. */
    @PutMapping("/api/secrets/{id}/environments/{environmentId}/value")
    @PreAuthorize("hasRole('READ_WRITE')")
    public ResponseEntity<Void> setValue(@PathVariable Long id, @PathVariable Long environmentId,
                                          @Valid @RequestBody ConfigurationValueRequest request,
                                          @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        configurationService.setValue(id, environmentId, request.value(), principal);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/api/secrets/{id}/environments/{environmentId}/value")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteValue(@PathVariable Long id, @PathVariable Long environmentId,
                                             @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        configurationService.deleteValue(id, environmentId, principal);
        return ResponseEntity.noContent().build();
    }

    /** History across every environment for this secret. Pass ?environmentId= to scope to just one. */
    @GetMapping("/api/secrets/{id}/history")
    @PreAuthorize("hasRole('READ_ONLY')")
    public List<ConfigurationValueHistoryViewModel> getHistory(@PathVariable Long id,
                                                          @RequestParam(required = false) Long environmentId) {
        return configurationService.getHistory(id, environmentId).stream().map(ConfigurationValueHistoryViewModel::from).toList();
    }

    /** Decrypts the value as of one specific history entry — same READ_WRITE+ restriction as the current-value reveal endpoint. */
    @GetMapping("/api/secrets/{id}/history/{historyId}/value")
    @PreAuthorize("hasRole('READ_WRITE')")
    public HistoricConfigurationValueViewModel revealHistoricValue(@PathVariable Long id, @PathVariable Long historyId) {
        String value = configurationService.revealHistoricValue(id, historyId);
        return new HistoricConfigurationValueViewModel(historyId, value);
    }
}

