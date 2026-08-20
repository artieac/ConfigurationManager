package com.alwaysmoveforward.configurationmanager.web.Models;

import com.alwaysmoveforward.configurationmanager.domainmodel.ApiKey;

import java.time.Instant;

/** Metadata only — never the token or its hash. See IssuedApiKeyViewModel for the one-time creation response. */
public record ApiKeyViewModel(Long id, Long systemId, String name, ChangeStampViewModel created, Instant lastUsedAt) {

    public static ApiKeyViewModel from(ApiKey apiKey) {
        return new ApiKeyViewModel(apiKey.id(), apiKey.systemId(), apiKey.name(),
                ChangeStampViewModel.from(apiKey.created()), apiKey.lastUsedAt());
    }
}

