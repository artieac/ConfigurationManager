package com.alwaysmoveforward.configurationmanager.domainmodel;

/**
 * The three access tiers in the system, ordered from least to most privileged.
 * Ordinal order is significant: {@link #atLeast(Role)} relies on it, and it
 * mirrors the ADMIN &gt; READ_WRITE &gt; READ_ONLY Spring Security role hierarchy.
 */
public enum Role {

    READ_ONLY,
    READ_WRITE,
    ADMIN;

    /** Spring Security authority name, e.g. "ROLE_ADMIN". */
    public String authority() {
        return "ROLE_" + name();
    }

    /** True if this role's privilege level is at least as high as {@code other}. */
    public boolean atLeast(Role other) {
        return this.ordinal() >= other.ordinal();
    }

    public static Role fromAuthorityName(String name) {
        for (Role role : values()) {
            if (role.name().equalsIgnoreCase(name) || role.authority().equalsIgnoreCase(name)) {
                return role;
            }
        }
        throw new IllegalArgumentException("Unknown role: " + name);
    }
}

