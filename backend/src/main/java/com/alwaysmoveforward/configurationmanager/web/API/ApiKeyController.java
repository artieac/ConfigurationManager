package com.alwaysmoveforward.configurationmanager.web.API;

import com.alwaysmoveforward.configurationmanager.security.jwt.AuthenticatedPrincipal;
import com.alwaysmoveforward.configurationmanager.services.ApiKeyService;
import com.alwaysmoveforward.configurationmanager.web.Models.ApiKeyRequest;
import com.alwaysmoveforward.configurationmanager.web.Models.ApiKeyViewModel;
import com.alwaysmoveforward.configurationmanager.web.Models.IssuedApiKeyViewModel;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Managing API keys follows the same tiered model as everything else: READ_ONLY can see which
 * keys exist (never the token), READ_WRITE can create/rename them, and only ADMIN can revoke one
 * (a machine credential is treated like any other destructive-capable grant).
 */
@RestController
@RequestMapping("/api/systems/{systemId}/api-keys")
public class ApiKeyController extends ControllerBase {

    private final ApiKeyService apiKeyService;

    public ApiKeyController(ApiKeyService apiKeyService) {
        this.apiKeyService = apiKeyService;
    }

    @GetMapping
    @PreAuthorize("hasRole('READ_ONLY')")
    public List<ApiKeyViewModel> listApiKeys(@PathVariable Long systemId) {
        return apiKeyService.listApiKeys(systemId).stream().map(ApiKeyViewModel::from).toList();
    }

    /** The response's `token` field is the only time the raw key is ever returned — it can't be retrieved again. */
    @PostMapping
    @PreAuthorize("hasRole('READ_WRITE')")
    public ResponseEntity<IssuedApiKeyViewModel> createApiKey(@PathVariable Long systemId,
                                                                @Valid @RequestBody ApiKeyRequest request,
                                                                @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        var issued = apiKeyService.createApiKey(systemId, request.name(), principal);
        return ResponseEntity.status(201).body(IssuedApiKeyViewModel.from(issued));
    }

    /** Renames the key's label only — the token/hash is immutable, so there's nothing else to "update". */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('READ_WRITE')")
    public ApiKeyViewModel renameApiKey(@PathVariable Long systemId, @PathVariable Long id,
                                         @Valid @RequestBody ApiKeyRequest request) {
        var renamed = apiKeyService.renameApiKey(systemId, id, request.name());
        return ApiKeyViewModel.from(renamed);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> revokeApiKey(@PathVariable Long systemId, @PathVariable Long id) {
        apiKeyService.revokeApiKey(systemId, id);
        return ResponseEntity.noContent().build();
    }
}

