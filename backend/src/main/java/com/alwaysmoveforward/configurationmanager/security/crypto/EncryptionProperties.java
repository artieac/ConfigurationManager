package com.alwaysmoveforward.configurationmanager.security.crypto;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** {@code key} is a base64-encoded 256-bit AES key, e.g. from {@code openssl rand -base64 32}. */
@ConfigurationProperties(prefix = "configuration-manager.encryption")
public record EncryptionProperties(String key, int keyVersion) {
}

