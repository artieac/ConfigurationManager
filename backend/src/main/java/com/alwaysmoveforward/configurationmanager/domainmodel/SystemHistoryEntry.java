package com.alwaysmoveforward.configurationmanager.domainmodel;

/**
 * One append-only audit record of a change to a system — mirrors
 * {@link ConfigurationValueHistoryEntry} but carries no encrypted-content snapshot, since
 * a system holds no secret value of its own. Snapshots the system's full
 * field set (not just the name) so the UI can diff a row against the one
 * before it to show what actually changed, not just that something did.
 */
public class SystemHistoryEntry {

    private final Long id;
    private final Long systemId;
    private final String systemName;
    private final String externalId;
    private final String description;
    private final HistoryAction action;
    private final ChangeMetadata changed;

    public SystemHistoryEntry(Long id, Long systemId, String systemName, String externalId, String description,
                               HistoryAction action, ChangeMetadata changed) {
        this.id = id;
        this.systemId = systemId;
        this.systemName = systemName;
        this.externalId = externalId;
        this.description = description;
        this.action = action;
        this.changed = changed;
    }

    public static SystemHistoryEntry recordChange(ConfigurationSystem system, HistoryAction action, ChangeMetadata changedBy) {
        return new SystemHistoryEntry(null, system.id(), system.name(), system.externalId(), system.description(), action, changedBy);
    }

    public Long id() {
        return id;
    }

    public Long systemId() {
        return systemId;
    }

    public String systemName() {
        return systemName;
    }

    public String externalId() {
        return externalId;
    }

    public String description() {
        return description;
    }

    public HistoryAction action() {
        return action;
    }

    public ChangeMetadata changed() {
        return changed;
    }
}

