package com.alwaysmoveforward.configurationmanager.data.Entities;

import com.alwaysmoveforward.configurationmanager.domainmodel.HistoryAction;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "configuration_value_history")
public class ConfigurationValueHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "configuration_id")
    private ConfigurationEntity configuration;

    @ManyToOne
    @JoinColumn(name = "system_id")
    private SystemEntity system;

    @ManyToOne
    @JoinColumn(name = "environment_id")
    private EnvironmentEntity environment;

    @Column(name = "configuration_name", nullable = false)
    private String configurationName;

    @Column(name = "system_name", nullable = false)
    private String systemName;

    @Column(name = "environment_name", nullable = false, length = 100)
    private String environmentName;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 16)
    private HistoryAction action;

    @Column(name = "encrypted_value_snapshot", columnDefinition = "TEXT")
    private String encryptedValueSnapshot;

    @Column(name = "encryption_iv_snapshot", length = 64)
    private String encryptionIvSnapshot;

    @Column(name = "key_version")
    private Integer keyVersion;

    @ManyToOne(optional = false)
    @JoinColumn(name = "changed_by", nullable = false)
    private UserEntity changedBy;

    @CreationTimestamp
    @Column(name = "changed_at", nullable = false, updatable = false)
    private Instant changedAt;

    protected ConfigurationValueHistoryEntity() {
    }

    public ConfigurationValueHistoryEntity(ConfigurationEntity configuration, SystemEntity system, EnvironmentEntity environment,
                                     String configurationName, String systemName, String environmentName, HistoryAction action,
                                     String encryptedValueSnapshot, String encryptionIvSnapshot, Integer keyVersion,
                                     UserEntity changedBy) {
        this.configuration = configuration;
        this.system = system;
        this.environment = environment;
        this.configurationName = configurationName;
        this.systemName = systemName;
        this.environmentName = environmentName;
        this.action = action;
        this.encryptedValueSnapshot = encryptedValueSnapshot;
        this.encryptionIvSnapshot = encryptionIvSnapshot;
        this.keyVersion = keyVersion;
        this.changedBy = changedBy;
    }

    public Long getId() {
        return id;
    }

    public ConfigurationEntity getConfiguration() {
        return configuration;
    }

    public SystemEntity getSystem() {
        return system;
    }

    public EnvironmentEntity getEnvironment() {
        return environment;
    }

    public String getConfigurationName() {
        return configurationName;
    }

    public String getSystemName() {
        return systemName;
    }

    public String getEnvironmentName() {
        return environmentName;
    }

    public HistoryAction getAction() {
        return action;
    }

    public String getEncryptedValueSnapshot() {
        return encryptedValueSnapshot;
    }

    public String getEncryptionIvSnapshot() {
        return encryptionIvSnapshot;
    }

    public Integer getKeyVersion() {
        return keyVersion;
    }

    public UserEntity getChangedBy() {
        return changedBy;
    }

    public Instant getChangedAt() {
        return changedAt;
    }
}

