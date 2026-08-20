package com.alwaysmoveforward.configurationmanager.data.Entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "configuration_values")
public class ConfigurationValueEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "configuration_id", nullable = false)
    private ConfigurationEntity configuration;

    @ManyToOne(optional = false)
    @JoinColumn(name = "environment_id", nullable = false)
    private EnvironmentEntity environment;

    @Column(name = "encrypted_value", nullable = false, columnDefinition = "TEXT")
    private String encryptedValue;

    @Column(name = "encryption_iv", nullable = false, length = 64)
    private String encryptionIv;

    @Column(name = "key_version", nullable = false)
    private int keyVersion;

    @ManyToOne(optional = false)
    @JoinColumn(name = "created_by", nullable = false, updatable = false)
    private UserEntity createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ConfigurationValueEntity() {
    }

    public ConfigurationValueEntity(ConfigurationEntity configuration, EnvironmentEntity environment, String encryptedValue,
                              String encryptionIv, int keyVersion, UserEntity createdBy) {
        this.configuration = configuration;
        this.environment = environment;
        this.encryptedValue = encryptedValue;
        this.encryptionIv = encryptionIv;
        this.keyVersion = keyVersion;
        this.createdBy = createdBy;
    }

    public Long getId() {
        return id;
    }

    public ConfigurationEntity getConfiguration() {
        return configuration;
    }

    public EnvironmentEntity getEnvironment() {
        return environment;
    }

    public String getEncryptedValue() {
        return encryptedValue;
    }

    public void setEncryptedValue(String encryptedValue) {
        this.encryptedValue = encryptedValue;
    }

    public String getEncryptionIv() {
        return encryptionIv;
    }

    public void setEncryptionIv(String encryptionIv) {
        this.encryptionIv = encryptionIv;
    }

    public int getKeyVersion() {
        return keyVersion;
    }

    public void setKeyVersion(int keyVersion) {
        this.keyVersion = keyVersion;
    }

    public UserEntity getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

