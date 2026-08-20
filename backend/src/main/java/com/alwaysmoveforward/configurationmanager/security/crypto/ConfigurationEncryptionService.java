package com.alwaysmoveforward.configurationmanager.security.crypto;

import com.alwaysmoveforward.configurationmanager.domainmodel.EncryptedConfigurationValue;
import com.alwaysmoveforward.configurationmanager.services.ConfigurationEncryptor;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Encrypts/decrypts secret values with AES-256-GCM. A fresh random 96-bit IV is
 * generated for every {@link #encrypt} call — including re-encrypting an
 * unchanged value on update — because GCM's confidentiality guarantee depends
 * on never reusing an (key, IV) pair. Plaintext never leaves this class except
 * as the return value of {@link #decrypt}, which callers must treat as
 * short-lived (never logged, never persisted).
 */
@Service
public class ConfigurationEncryptionService implements ConfigurationEncryptor {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH_BYTES = 12;
    private static final int GCM_TAG_LENGTH_BITS = 128;

    private final SecretKeySpec key;
    private final int keyVersion;
    private final SecureRandom secureRandom = new SecureRandom();

    public ConfigurationEncryptionService(EncryptionProperties properties) {
        if (properties.key() == null || properties.key().isBlank()) {
            throw new IllegalStateException(
                    "configuration-manager.encryption.key (env SECRET_ENCRYPTION_KEY) must be set to a base64-encoded 256-bit key");
        }
        byte[] keyBytes = Base64.getDecoder().decode(properties.key());
        if (keyBytes.length != 32) {
            throw new IllegalStateException("configuration-manager.encryption.key must decode to exactly 32 bytes (AES-256)");
        }
        this.key = new SecretKeySpec(keyBytes, "AES");
        this.keyVersion = properties.keyVersion() > 0 ? properties.keyVersion() : 1;
    }

    public EncryptedConfigurationValue encrypt(String plaintext) {
        try {
            byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(java.nio.charset.StandardCharsets.UTF_8));

            return new EncryptedConfigurationValue(
                    Base64.getEncoder().encodeToString(ciphertext),
                    Base64.getEncoder().encodeToString(iv),
                    keyVersion);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Failed to encrypt secret value", e);
        }
    }

    public String decrypt(EncryptedConfigurationValue value) {
        try {
            byte[] iv = Base64.getDecoder().decode(value.ivBase64());
            byte[] ciphertext = Base64.getDecoder().decode(value.ciphertextBase64());

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            byte[] plaintext = cipher.doFinal(ciphertext);

            return new String(plaintext, java.nio.charset.StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Failed to decrypt secret value — ciphertext may be corrupt or tampered with", e);
        }
    }
}


