package com.alwaysmoveforward.configurationmanager.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/** Origins allowed to call this API with credentials — see SecurityConfig#corsConfigurationSource. */
@ConfigurationProperties(prefix = "configuration-manager.cors")
public record CorsProperties(List<String> allowedOrigins) {
}

