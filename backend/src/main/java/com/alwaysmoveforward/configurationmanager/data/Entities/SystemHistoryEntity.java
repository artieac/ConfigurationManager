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
@Table(name = "system_history")
public class SystemHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "system_id")
    private SystemEntity system;

    @Column(name = "system_name", nullable = false)
    private String systemName;

    @Column(name = "external_id")
    private String externalId;

    @Column(name = "description")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 16)
    private HistoryAction action;

    @ManyToOne(optional = false)
    @JoinColumn(name = "changed_by", nullable = false)
    private UserEntity changedBy;

    @CreationTimestamp
    @Column(name = "changed_at", nullable = false, updatable = false)
    private Instant changedAt;

    protected SystemHistoryEntity() {
    }

    public SystemHistoryEntity(SystemEntity system, String systemName, String externalId, String description,
                                HistoryAction action, UserEntity changedBy) {
        this.system = system;
        this.systemName = systemName;
        this.externalId = externalId;
        this.description = description;
        this.action = action;
        this.changedBy = changedBy;
    }

    public Long getId() {
        return id;
    }

    public SystemEntity getSystem() {
        return system;
    }

    public String getSystemName() {
        return systemName;
    }

    public String getExternalId() {
        return externalId;
    }

    public String getDescription() {
        return description;
    }

    public HistoryAction getAction() {
        return action;
    }

    public UserEntity getChangedBy() {
        return changedBy;
    }

    public Instant getChangedAt() {
        return changedAt;
    }
}

