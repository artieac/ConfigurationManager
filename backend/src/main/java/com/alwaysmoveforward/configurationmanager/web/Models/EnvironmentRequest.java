package com.alwaysmoveforward.configurationmanager.web.Models;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record EnvironmentRequest(
        @NotBlank @Size(max = 100) String name,
        // Used as a URL path segment by the API-key bulk-reveal endpoint (GET
        // /api/systems/{systemExternalId}/environments/{environmentExternalId}/secrets),
        // so it's restricted to characters that need no percent-encoding. Left
        // blank, the service defaults it to the environment's name.
        @Size(max = 255)
        @Pattern(regexp = "^$|^[A-Za-z0-9][A-Za-z0-9_-]*$", message = "must start with a letter or digit and contain only letters, digits, hyphens, and underscores")
        String externalId) {
}

