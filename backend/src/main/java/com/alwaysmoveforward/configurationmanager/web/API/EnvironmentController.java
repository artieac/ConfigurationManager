package com.alwaysmoveforward.configurationmanager.web.API;

import com.alwaysmoveforward.configurationmanager.security.jwt.AuthenticatedPrincipal;
import com.alwaysmoveforward.configurationmanager.services.EnvironmentService;
import com.alwaysmoveforward.configurationmanager.web.Models.EnvironmentRequest;
import com.alwaysmoveforward.configurationmanager.web.Models.EnvironmentViewModel;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class EnvironmentController extends ControllerBase {

    private final EnvironmentService environmentService;

    public EnvironmentController(EnvironmentService environmentService) {
        this.environmentService = environmentService;
    }

    @GetMapping("/api/systems/{systemId}/environments")
    @PreAuthorize("hasRole('READ_ONLY')")
    public List<EnvironmentViewModel> listEnvironments(@PathVariable Long systemId) {
        return environmentService.listEnvironments(systemId).stream().map(EnvironmentViewModel::from).toList();
    }

    @PostMapping("/api/systems/{systemId}/environments")
    @PreAuthorize("hasRole('READ_WRITE')")
    public ResponseEntity<EnvironmentViewModel> createEnvironment(@PathVariable Long systemId,
                                                                    @Valid @RequestBody EnvironmentRequest request,
                                                                    @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        var created = environmentService.createEnvironment(systemId, request.name(), request.externalId(), principal);
        return ResponseEntity.status(201).body(EnvironmentViewModel.from(created));
    }

    @PutMapping("/api/environments/{id}")
    @PreAuthorize("hasRole('READ_WRITE')")
    public EnvironmentViewModel renameEnvironment(@PathVariable Long id, @Valid @RequestBody EnvironmentRequest request,
                                                   @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        return EnvironmentViewModel.from(environmentService.renameEnvironment(id, request.name(), request.externalId(), principal));
    }

    @DeleteMapping("/api/environments/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteEnvironment(@PathVariable Long id, @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        environmentService.deleteEnvironment(id, principal);
        return ResponseEntity.noContent().build();
    }
}

