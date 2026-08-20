package com.alwaysmoveforward.configurationmanager.domainmodel;

/**
 * The encrypted value a {@link Configuration} holds within one specific
 * {@link Environment}. A secret can be sparse — present in a system but with
 * no value yet set for some of its environments — so not every (secret,
 * environment) pair has a row.
 *
 * Only tracks who first set it and when — {@link ConfigurationValueHistoryEntry}
 * records every subsequent create/update/delete, so a separate "last updated"
 * stamp on this object would just duplicate what history already knows.
 */
public class ConfigurationValue {

    private final Long id;
    private final Long configurationId;
    private final Long environmentId;
    private final EncryptedConfigurationValue value;
    private final ChangeMetadata created;

    public ConfigurationValue(Long id, Long configurationId, Long environmentId, EncryptedConfigurationValue value, ChangeMetadata created) {
        this.id = id;
        this.configurationId = configurationId;
        this.environmentId = environmentId;
        this.value = value;
        this.created = created;
    }

    public static ConfigurationValue newValue(Long configurationId, Long environmentId, EncryptedConfigurationValue value, ChangeMetadata createdBy) {
        return new ConfigurationValue(null, configurationId, environmentId, value, createdBy);
    }

    public ConfigurationValue withUpdatedValue(EncryptedConfigurationValue newValue) {
        return new ConfigurationValue(id, configurationId, environmentId, newValue, created);
    }

    public Long id() {
        return id;
    }

    public Long configurationId() {
        return configurationId;
    }

    public Long environmentId() {
        return environmentId;
    }

    public EncryptedConfigurationValue value() {
        return value;
    }

    public ChangeMetadata created() {
        return created;
    }
}

