package com.alwaysmoveforward.configurationmanager.data.mapper;

import com.alwaysmoveforward.configurationmanager.data.Entities.RoleEntity;
import com.alwaysmoveforward.configurationmanager.data.Entities.UserEntity;
import com.alwaysmoveforward.configurationmanager.domainmodel.Role;
import com.alwaysmoveforward.configurationmanager.domainmodel.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public User toDomain(UserEntity entity) {
        return new User(
                entity.getId(),
                entity.getAuth0UserId(),
                entity.getEmail(),
                entity.getDisplayName(),
                Role.valueOf(entity.getRole().getName()),
                entity.isActive(),
                entity.getCreatedAt(),
                entity.getLastLoginAt());
    }

    public UserEntity toNewEntity(User user, RoleEntity roleEntity) {
        return new UserEntity(user.auth0UserId(), user.email(), user.displayName(), roleEntity,
                user.active(), user.lastLoginAt());
    }

    public void applyToEntity(UserEntity entity, User user, RoleEntity roleEntity) {
        entity.setEmail(user.email());
        entity.setDisplayName(user.displayName());
        entity.setRole(roleEntity);
        entity.setLastLoginAt(user.lastLoginAt());
    }
}

