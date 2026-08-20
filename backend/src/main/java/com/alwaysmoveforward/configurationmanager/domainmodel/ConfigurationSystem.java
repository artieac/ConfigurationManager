package com.alwaysmoveforward.configurationmanager.domainmodel;

/**
 * A named container of secrets (e.g. "Payments API"). Named {@code ConfigurationSystem}
 * rather than {@code System} to avoid shadowing {@link java.lang.System}.
 *
 * Only tracks who created it and when — {@code SystemHistoryEntry} records
 * every subsequent rename/delete, so a separate "last updated" stamp on this
 * object would just duplicate what history already knows.
 */
public class ConfigurationSystem {

    private final Long id;
    private final String name;
    private final String externalId;
    private final String description;
    private final ChangeMetadata created;

    public ConfigurationSystem(Long id, String name, String externalId, String description, ChangeMetadata created) {
        this.id = id;
        this.name = name;
        this.externalId = externalId;
        this.description = description;
        this.created = created;
    }

    public static ConfigurationSystem newSystem(String name, String externalId, String description, ChangeMetadata createdBy) {
        return new ConfigurationSystem(null, name, externalId, description, createdBy);
    }

    public ConfigurationSystem withUpdatedDetails(String newName, String newExternalId, String newDescription) {
        return new ConfigurationSystem(id, newName, newExternalId, newDescription, created);
    }

    public Long id() {
        return id;
    }

    public String name() {
        return name;
    }

    public String externalId() {
        return externalId;
    }

    public String description() {
        return description;
    }

    public ChangeMetadata created() {
        return created;
    }
}

