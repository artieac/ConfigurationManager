package com.alwaysmoveforward.configurationmanager.web.API;

import com.alwaysmoveforward.configurationmanager.security.jwt.AuthenticatedPrincipal;
import com.alwaysmoveforward.configurationmanager.services.SystemService;
import com.alwaysmoveforward.configurationmanager.web.Models.SystemHistoryViewModel;
import com.alwaysmoveforward.configurationmanager.web.Models.SystemRequest;
import com.alwaysmoveforward.configurationmanager.web.Models.SystemViewModel;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/systems")
public class SystemController extends ControllerBase {

    private final SystemService systemService;

    public SystemController(SystemService systemService) {
        this.systemService = systemService;
    }

    @GetMapping
    @PreAuthorize("hasRole('READ_ONLY')")
    public List<SystemViewModel> listSystems() {
        return systemService.listSystems().stream().map(SystemViewModel::from).toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('READ_ONLY')")
    public SystemViewModel getSystem(@PathVariable Long id) {
        return SystemViewModel.from(systemService.getSystem(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('READ_WRITE')")
    public ResponseEntity<SystemViewModel> createSystem(@Valid @RequestBody SystemRequest request,
                                                          @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        var created = systemService.createSystem(request.name(), request.externalId(), request.description(), principal);
        return ResponseEntity.status(201).body(SystemViewModel.from(created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('READ_WRITE')")
    public SystemViewModel updateSystem(@PathVariable Long id, @Valid @RequestBody SystemRequest request,
                                         @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        return SystemViewModel.from(systemService.updateSystem(id, request.name(), request.externalId(), request.description(), principal));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteSystem(@PathVariable Long id, @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        systemService.deleteSystem(id, principal);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/history")
    @PreAuthorize("hasRole('READ_ONLY')")
    public List<SystemHistoryViewModel> getHistory(@PathVariable Long id) {
        return systemService.getHistory(id).stream().map(SystemHistoryViewModel::from).toList();
    }
}

