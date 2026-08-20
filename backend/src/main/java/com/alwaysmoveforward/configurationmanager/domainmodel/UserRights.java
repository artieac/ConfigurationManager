package com.alwaysmoveforward.configurationmanager.domainmodel;

/**
 * Policy object translating a {@link Role} into intention-revealing permission
 * checks, so business rules about "who can do what" live in one place rather
 * than as scattered role-name comparisons across services/controllers.
 */
public class UserRights {

    private final Role role;

    public UserRights(Role role) {
        this.role = role;
    }

    public Role role() {
        return role;
    }

    /** View systems, secret names/metadata, and secret history. Every authenticated role can. */
    public boolean canView() {
        return role.atLeast(Role.READ_ONLY);
    }

    /** Create/update systems and secrets. */
    public boolean canWrite() {
        return role.atLeast(Role.READ_WRITE);
    }

    /** Decrypt and view a secret's actual value. */
    public boolean canRevealConfigurationValue() {
        return role.atLeast(Role.READ_WRITE);
    }

    /** Delete systems or secrets. */
    public boolean canDelete() {
        return role.atLeast(Role.ADMIN);
    }

    /** Change another user's role assignment. */
    public boolean canManageUsers() {
        return role.atLeast(Role.ADMIN);
    }
}

