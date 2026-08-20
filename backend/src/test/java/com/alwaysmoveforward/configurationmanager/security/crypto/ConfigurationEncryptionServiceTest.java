package com.alwaysmoveforward.configurationmanager.security.crypto;

import com.alwaysmoveforward.configurationmanager.domainmodel.EncryptedConfigurationValue;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConfigurationEncryptionServiceTest {

    private final ConfigurationEncryptionService service =
            new ConfigurationEncryptionService(new EncryptionProperties(Base64.getEncoder().encodeToString(new byte[32]), 1));

    @Test
    void encryptThenDecryptReturnsOriginalPlaintext() {
        EncryptedConfigurationValue encrypted = service.encrypt("super-secret-password");

        assertEquals("super-secret-password", service.decrypt(encrypted));
    }

    @Test
    void encryptingTheSameValueTwiceProducesDifferentIvsAndCiphertext() {
        EncryptedConfigurationValue first = service.encrypt("same-value");
        EncryptedConfigurationValue second = service.encrypt("same-value");

        assertNotEquals(first.ivBase64(), second.ivBase64());
        assertNotEquals(first.ciphertextBase64(), second.ciphertextBase64());
    }

    @Test
    void tamperedCiphertextFailsToDecrypt() {
        EncryptedConfigurationValue encrypted = service.encrypt("super-secret-password");
        byte[] tampered = Base64.getDecoder().decode(encrypted.ciphertextBase64());
        tampered[0] ^= 0x01;
        EncryptedConfigurationValue withTamperedCiphertext = new EncryptedConfigurationValue(
                Base64.getEncoder().encodeToString(tampered), encrypted.ivBase64(), encrypted.keyVersion());

        assertThrows(IllegalStateException.class, () -> service.decrypt(withTamperedCiphertext));
    }
}

