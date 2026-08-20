package com.alwaysmoveforward.configurationmanager.data.repositories;

import com.alwaysmoveforward.configurationmanager.data.Entities.RoleEntity;
import com.alwaysmoveforward.configurationmanager.data.Entities.UserEntity;
import com.alwaysmoveforward.configurationmanager.data.dao.RoleDAO;
import com.alwaysmoveforward.configurationmanager.data.dao.UserDAO;
import com.alwaysmoveforward.configurationmanager.data.mapper.UserMapper;
import com.alwaysmoveforward.configurationmanager.domainmodel.Role;
import com.alwaysmoveforward.configurationmanager.domainmodel.User;
import com.alwaysmoveforward.configurationmanager.exceptions.NotFoundException;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class UserRepository extends RepositoryBase {

    private final UserDAO userDAO;
    private final RoleDAO roleDAO;
    private final UserMapper userMapper;

    public UserRepository(UserDAO userDAO, RoleDAO roleDAO, UserMapper userMapper) {
        this.userDAO = userDAO;
        this.roleDAO = roleDAO;
        this.userMapper = userMapper;
    }

    public Optional<User> findByAuth0UserId(String auth0UserId) {
        return userDAO.findByAuth0UserId(auth0UserId).map(userMapper::toDomain);
    }

    public User findById(Long id) {
        return orNotFound(userDAO.findById(id).map(userMapper::toDomain), () -> "User not found: " + id);
    }

    public List<User> findAll() {
        return userDAO.findAll().stream().map(userMapper::toDomain).toList();
    }

    public User save(User user) {
        RoleEntity roleEntity = roleEntityFor(user.role());

        if (user.id() == null) {
            UserEntity created = userDAO.save(userMapper.toNewEntity(user, roleEntity));
            return userMapper.toDomain(created);
        }

        UserEntity existing = userDAO.findById(user.id())
                .orElseThrow(() -> new NotFoundException("User not found: " + user.id()));
        userMapper.applyToEntity(existing, user, roleEntity);
        return userMapper.toDomain(userDAO.save(existing));
    }

    private RoleEntity roleEntityFor(Role role) {
        return roleDAO.findByName(role.name())
                .orElseThrow(() -> new NotFoundException("Role not seeded: " + role.name()));
    }
}

