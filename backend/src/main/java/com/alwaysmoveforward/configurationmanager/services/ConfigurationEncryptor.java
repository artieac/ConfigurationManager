package com.alwaysmoveforward.configurationmanager.services;

import com.alwaysmoveforward.configurationmanager.domainmodel.EncryptedConfigurationValue;

/**
 * Port for encrypting and decrypting secret values. The service layer depends
 * on this interface rather than the concrete {@code ConfigurationEncryptionService}
 * so that the AES-GCM implementation can be swapped out (e.g. for a cloud KMS
 * adapter) without touching any service code.
 *
 * <p>Implementations MUST ensure that plaintext is never logged or persisted
 * anywhere. Treat the return value of {@link #decrypt} as short-lived.
 */
public interface ConfigurationEncryptor {

    EncryptedConfigurationValue encrypt(String plaintext);

    String decrypt(EncryptedConfigurationValue value);
}

