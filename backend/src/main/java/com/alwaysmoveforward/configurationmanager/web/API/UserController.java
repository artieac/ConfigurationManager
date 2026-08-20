package com.alwaysmoveforward.configurationmanager.web.API;

import com.alwaysmoveforward.configurationmanager.domainmodel.Role;
import com.alwaysmoveforward.configurationmanager.services.UserService;
import com.alwaysmoveforward.configurationmanager.web.Models.UpdateUserRoleRequest;
import com.alwaysmoveforward.configurationmanager.web.Models.UserViewModel;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** User/role management — every endpoint here is ADMIN-only. */
@RestController
@RequestMapping("/api/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserController extends ControllerBase {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public List<UserViewModel> listUsers() {
        return userService.listUsers().stream().map(UserViewModel::from).toList();
    }

    @PutMapping("/{id}/role")
    public UserViewModel updateRole(@PathVariable Long id, @Valid @RequestBody UpdateUserRoleRequest request) {
        Role role = Role.fromAuthorityName(request.role());
        return UserViewModel.from(userService.updateUserRole(id, role));
    }
}

