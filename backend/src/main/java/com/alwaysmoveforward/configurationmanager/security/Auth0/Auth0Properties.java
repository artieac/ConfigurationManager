package com.alwaysmoveforward.configurationmanager.security.Auth0;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "configuration-manager.auth0")
public record Auth0Properties(String domain, String clientId, String clientSecret, String callbackUrl, String audience, String logoutUrl) {
}

