package com.alwaysmoveforward.configurationmanager.domainmodel;

/**
 * One append-only audit record of a change to a secret's value within one
 * environment. Never carries the value itself, encrypted or otherwise — the
 * encrypted snapshot lives only in {@code data/} and reaches a client
 * exclusively through {@code ConfigurationValueService#revealHistoricValue}, one
 * entry at a time, on explicit request (see {@code ConfigurationValueHistoryViewModel}
 * vs. {@code HistoricConfigurationValueViewModel}).
 */
public class ConfigurationValueHistoryEntry {

    private final Long id;
    private final Long configurationId;
    private final Long systemId;
    private final Long environmentId;
    private final String configurationName;
    private final String systemName;
    private final String environmentName;
    private final HistoryAction action;
    private final ChangeMetadata changed;

    public ConfigurationValueHistoryEntry(Long id, Long configurationId, Long systemId, Long environmentId, String configurationName,
                                    String systemName, String environmentName, HistoryAction action, ChangeMetadata changed) {
        this.id = id;
        this.configurationId = configurationId;
        this.systemId = systemId;
        this.environmentId = environmentId;
        this.configurationName = configurationName;
        this.systemName = systemName;
        this.environmentName = environmentName;
        this.action = action;
        this.changed = changed;
    }

    public static ConfigurationValueHistoryEntry recordChange(Configuration secret, String systemName, Environment environment,
                                                         HistoryAction action, ChangeMetadata changedBy) {
        return new ConfigurationValueHistoryEntry(null, secret.id(), secret.systemId(), environment.id(), secret.name(),
                systemName, environment.name(), action, changedBy);
    }

    public Long id() {
        return id;
    }

    public Long configurationId() {
        return configurationId;
    }

    public Long systemId() {
        return systemId;
    }

    public Long environmentId() {
        return environmentId;
    }

    public String configurationName() {
        return configurationName;
    }

    public String systemName() {
        return systemName;
    }

    public String environmentName() {
        return environmentName;
    }

    public HistoryAction action() {
        return action;
    }

    public ChangeMetadata changed() {
        return changed;
    }
}

