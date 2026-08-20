package com.alwaysmoveforward.configurationmanager.web.Models;

import com.alwaysmoveforward.configurationmanager.domainmodel.User;
import com.alwaysmoveforward.configurationmanager.domainmodel.UserRights;
import com.alwaysmoveforward.configurationmanager.security.jwt.AuthenticatedPrincipal;

/** Response for GET /api/auth/me — includes capability flags so the frontend can gate UI without duplicating role logic. */
public record CurrentUserViewModel(Long id, String email, String displayName, String role,
                                    boolean canWrite, boolean canRevealConfigurationValue, boolean canDelete, boolean canManageUsers) {

    public static CurrentUserViewModel from(User user) {
        return from(user.rights(), user.id(), user.email(), user.displayName(), user.role().name());
    }

    public static CurrentUserViewModel from(AuthenticatedPrincipal principal) {
        UserRights rights = new UserRights(principal.role());
        return from(rights, principal.userId(), principal.email(), principal.displayName(), principal.role().name());
    }

    private static CurrentUserViewModel from(UserRights rights, Long id, String email, String displayName, String role) {
        return new CurrentUserViewModel(id, email, displayName, role,
                rights.canWrite(), rights.canRevealConfigurationValue(), rights.canDelete(), rights.canManageUsers());
    }
}

