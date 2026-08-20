package com.alwaysmoveforward.configurationmanager.web.Models;

import com.alwaysmoveforward.configurationmanager.domainmodel.User;

import java.time.Instant;

public record UserViewModel(Long id, String email, String displayName, String role,
                             boolean active, Instant createdAt, Instant lastLoginAt) {

    public static UserViewModel from(User user) {
        return new UserViewModel(user.id(), user.email(), user.displayName(), user.role().name(),
                user.active(), user.createdAt(), user.lastLoginAt());
    }
}

