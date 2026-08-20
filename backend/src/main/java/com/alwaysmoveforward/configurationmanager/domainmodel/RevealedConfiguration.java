package com.alwaysmoveforward.configurationmanager.domainmodel;

/**
 * A decrypted secret value ready for consumption — produced by reveal operations and
 * the bulk environment-reveal endpoint. Short-lived: callers must never log or persist
 * this object or its {@code value} field.
 */
public record RevealedConfiguration(Long configurationId, String configurationName, String value) {
}

