package com.alwaysmoveforward.configurationmanager.domainmodel;

import java.time.Instant;

/**
 * A machine credential scoped to exactly one system, for pulling that
 * system's secrets without a browser session (e.g. a CI/CD deploy job).
 * Never carries the token or its hash — those exist only in the data layer,
 * used purely to look this record up during authentication (see
 * {@code ApiKeyRepository}/{@code ApiKeyAuthenticationFilter}). Once issued,
 * the raw token itself is never stored or recoverable.
 */
public class ApiKey {

    private final Long id;
    private final Long systemId;
    private final String name;
    private final ChangeMetadata created;
    private final Instant lastUsedAt;

    public ApiKey(Long id, Long systemId, String name, ChangeMetadata created, Instant lastUsedAt) {
        this.id = id;
        this.systemId = systemId;
        this.name = name;
        this.created = created;
        this.lastUsedAt = lastUsedAt;
    }

    public static ApiKey newKey(Long systemId, String name, ChangeMetadata createdBy) {
        return new ApiKey(null, systemId, name, createdBy, null);
    }

    public ApiKey withName(String newName) {
        return new ApiKey(id, systemId, newName, created, lastUsedAt);
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

    public Instant lastUsedAt() {
        return lastUsedAt;
    }
}

