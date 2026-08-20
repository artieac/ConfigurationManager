package com.alwaysmoveforward.configurationmanager.domainmodel;

import java.time.Instant;

public class User {

    private final Long id;
    private final String auth0UserId;
    private final String email;
    private final String displayName;
    private final Role role;
    private final boolean active;
    private final Instant createdAt;
    private final Instant lastLoginAt;

    public User(Long id, String auth0UserId, String email, String displayName, Role role,
                boolean active, Instant createdAt, Instant lastLoginAt) {
        this.id = id;
        this.auth0UserId = auth0UserId;
        this.email = email;
        this.displayName = displayName;
        this.role = role;
        this.active = active;
        this.createdAt = createdAt;
        this.lastLoginAt = lastLoginAt;
    }

    /** Creates the in-memory representation of a brand-new user on their first login. */
    public static User newFromAuth0Login(Auth0UserProfile profile, Role defaultRole) {
        return new User(null, profile.subject(), profile.email(), profile.name(), defaultRole,
                true, Instant.now(), Instant.now());
    }

    public User withLastLoginNow() {
        return new User(id, auth0UserId, email, displayName, role, active, createdAt, Instant.now());
    }

    /** Refreshes email/display name from a fresh Auth0 profile and stamps the login time — called on every login. */
    public User withProfileRefreshedFrom(Auth0UserProfile profile) {
        return new User(id, auth0UserId, profile.email(), profile.name(), role, active, createdAt, Instant.now());
    }

    public User withRole(Role newRole) {
        return new User(id, auth0UserId, email, displayName, newRole, active, createdAt, lastLoginAt);
    }

    public UserRights rights() {
        return new UserRights(role);
    }

    public Long id() {
        return id;
    }

    public String auth0UserId() {
        return auth0UserId;
    }

    public String email() {
        return email;
    }

    public String displayName() {
        return displayName;
    }

    public Role role() {
        return role;
    }

    public boolean active() {
        return active;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant lastLoginAt() {
        return lastLoginAt;
    }
}

