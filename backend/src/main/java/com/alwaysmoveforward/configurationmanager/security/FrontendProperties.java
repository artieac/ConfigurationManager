package com.alwaysmoveforward.configurationmanager.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** The single origin the frontend is served from — used for both CORS and the post-login redirect. */
@ConfigurationProperties(prefix = "configuration-manager.frontend")
public record FrontendProperties(String baseUrl) {
}

