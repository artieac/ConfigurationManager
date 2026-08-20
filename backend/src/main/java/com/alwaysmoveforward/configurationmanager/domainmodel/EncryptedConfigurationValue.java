package com.alwaysmoveforward.configurationmanager.domainmodel;

/**
 * The at-rest form of a secret's value: AES-256-GCM ciphertext+auth-tag and the
 * IV used to produce it, both base64-encoded, plus which key version encrypted
 * it. Never carries plaintext — decryption happens on demand in
 * {@code ConfigurationEncryptionService} and the plaintext is never wrapped in this type.
 */
public record EncryptedConfigurationValue(String ciphertextBase64, String ivBase64, int keyVersion) {
}

