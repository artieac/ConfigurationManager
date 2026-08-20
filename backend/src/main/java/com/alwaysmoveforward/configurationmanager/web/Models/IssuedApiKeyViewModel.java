package com.alwaysmoveforward.configurationmanager.web.Models;

import com.alwaysmoveforward.configurationmanager.services.ApiKeyService;

/**
 * The only response shape in the API that ever carries a raw API key token —
 * returned once, from creation, and never retrievable again afterward.
 */
public record IssuedApiKeyViewModel(Long id, Long systemId, String name, String token) {

    public static IssuedApiKeyViewModel from(ApiKeyService.IssuedApiKey issued) {
        return new IssuedApiKeyViewModel(issued.apiKey().id(), issued.apiKey().systemId(), issued.apiKey().name(), issued.token());
    }
}

