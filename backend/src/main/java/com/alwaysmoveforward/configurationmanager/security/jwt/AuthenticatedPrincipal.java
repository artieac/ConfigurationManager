package com.alwaysmoveforward.configurationmanager.security.jwt;

import com.alwaysmoveforward.configurationmanager.domainmodel.Role;

/** The identity carried inside the signed JWT cookie, once validated. */
public record AuthenticatedPrincipal(Long userId, String email, String displayName, Role role) {
}

