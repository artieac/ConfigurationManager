package com.alwaysmoveforward.configurationmanager.domainmodel;

/**
 * A secret is a NAME scoped to a system — it carries no value itself. Each of
 * the system's {@link Environment}s can hold its own {@link ConfigurationValue} for
 * this name (see that class), which is what lets the same secret carry a
 * different value per environment.
 */
public class Configuration {

    private final Long id;
    private final Long systemId;
    private final String name;
    private final ChangeMetadata created;
    private final ChangeMetadata updated;

    public Configuration(Long id, Long systemId, String name, ChangeMetadata created, ChangeMetadata updated) {
        this.id = id;
        this.systemId = systemId;
        this.name = name;
        this.created = created;
        this.updated = updated;
    }

    public static Configuration newConfiguration(Long systemId, String name, ChangeMetadata createdBy) {
        return new Configuration(null, systemId, name, createdBy, createdBy);
    }

    public Configuration withRename(String newName, ChangeMetadata updatedBy) {
        return new Configuration(id, systemId, newName, created, updatedBy);
    }

    public Long id() {
        return id;
    }

    public Long systemId() {
        return systemId;
    }

    public String name() {
        return name;
    }

    public ChangeMetadata created() {
        return created;
    }

    public ChangeMetadata updated() {
        return updated;
    }
}

