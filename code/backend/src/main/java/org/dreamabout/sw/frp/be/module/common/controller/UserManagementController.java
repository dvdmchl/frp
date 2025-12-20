package org.dreamabout.sw.frp.be.module.common.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.dreamabout.sw.frp.be.domain.ApiPath;
import org.dreamabout.sw.frp.be.module.common.model.dto.UserDto;
import org.dreamabout.sw.frp.be.module.common.model.dto.UserUpdateGroupsRequestDto;
import org.dreamabout.sw.frp.be.module.common.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPath.ADMIN_USERS_FULL)
@Tag(name = "User Management (Admin)", description = "Administrative operations for user management")
@PreAuthorize("hasRole('ADMIN')")
public class UserManagementController {

    private final UserService userService;

    @GetMapping
    @Operation(summary = "List/Search users", description = "Returns all users or searches by email/name.")
    public ResponseEntity<List<UserDto>> listUsers(@RequestParam(required = false) String query) {
        if (query != null && !query.isBlank()) {
            return ResponseEntity.ok(userService.searchUsers(query));
        }
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping(ApiPath.ID_PARAM)
    @Operation(summary = "Get user details", description = "Returns detailed information about a user.")
    public ResponseEntity<UserDto> getUser(@PathVariable Long id) {
        return userService.getUserById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping(ApiPath.ID_ACTIVE)
    @Operation(summary = "Activate/Deactivate user", description = "Toggles the user's active status.")
    public ResponseEntity<UserDto> updateActiveStatus(@PathVariable Long id, @RequestParam boolean active) {
        return ResponseEntity.ok(userService.updateUserActiveStatus(id, active));
    }

    @PatchMapping(ApiPath.ID_ADMIN)
    @Operation(summary = "Grant/Revoke admin status", description = "Toggles the user's admin privilege.")
    public ResponseEntity<UserDto> updateAdminStatus(@PathVariable Long id, @RequestParam boolean admin) {
        return ResponseEntity.ok(userService.updateUserAdminStatus(id, admin));
    }

    @PutMapping(ApiPath.ID_GROUPS)
    @Operation(summary = "Update user groups", description = "Sets the groups for a user.")
    public ResponseEntity<UserDto> updateGroups(@PathVariable Long id, @RequestBody UserUpdateGroupsRequestDto request) {
        return ResponseEntity.ok(userService.updateUserGroups(id, request.groupIds()));
    }
}
