package com.alwaysmoveforward.configurationmanager.domainmodel;

/** A named deployment stage within a system (e.g. "Development", "Staging", "Production"). */
public class Environment {

    private final Long id;
    private final Long systemId;
    private final String name;
    private final String externalId;
    private final ChangeMetadata created;
    private final ChangeMetadata updated;

    public Environment(Long id, Long systemId, String name, String externalId, ChangeMetadata created, ChangeMetadata updated) {
        this.id = id;
        this.systemId = systemId;
        this.name = name;
        this.externalId = externalId;
        this.created = created;
        this.updated = updated;
    }

    public static Environment newEnvironment(Long systemId, String name, String externalId, ChangeMetadata createdBy) {
        return new Environment(null, systemId, name, externalId, createdBy, createdBy);
    }

    public Environment withRename(String newName, String newExternalId, ChangeMetadata updatedBy) {
        return new Environment(id, systemId, newName, newExternalId, created, updatedBy);
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

    public String externalId() {
        return externalId;
    }

    public ChangeMetadata created() {
        return created;
    }

    public ChangeMetadata updated() {
        return updated;
    }
}

