package org.dreamabout.sw.frp.be.module.common.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.dreamabout.sw.frp.be.domain.ApiPath;
import org.dreamabout.sw.frp.be.module.common.model.dto.UserDto;
import org.dreamabout.sw.frp.be.module.common.model.dto.UserLoginRequestDto;
import org.dreamabout.sw.frp.be.module.common.model.dto.UserLoginResponseDto;
import org.dreamabout.sw.frp.be.module.common.model.dto.UserRegisterRequestDto;
import org.dreamabout.sw.frp.be.module.common.model.dto.UserUpdateRequestDto;
import org.dreamabout.sw.frp.be.module.common.model.dto.UserUpdateInfoRequestDto;
import org.dreamabout.sw.frp.be.module.common.model.dto.UserChangePasswordRequestDto;
import org.dreamabout.sw.frp.be.module.common.service.JwtService;
import org.dreamabout.sw.frp.be.module.common.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPath.API_ROOT + ApiPath.USER)
@Tag(name = "User Management", description = "Operations related to user accounts")
public class UserController {

    private final UserService userService;
    private final JwtService jwtService;

    @Operation(summary = "Register new user", description = "Creates a new user account.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User registered successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input", content = @Content),
            @ApiResponse(responseCode = "409", description = "User with email already exists", content = @Content)
    })
    @PostMapping(ApiPath.USER_REGISTER)
    public ResponseEntity<UserDto> register(@Valid @RequestBody UserRegisterRequestDto userRegister) {
        return ResponseEntity.ok(userService.signup(userRegister));
    }

    @Operation(summary = "Authenticate user", description = "Logs in a user and returns user info.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Login successful"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials", content = @Content)
    })
    @PostMapping(ApiPath.USER_LOGIN)
    public ResponseEntity<UserLoginResponseDto> login(@Valid @RequestBody UserLoginRequestDto userLogin) {
        return ResponseEntity.ok(userService.authenticate(userLogin));
    }

    @Operation(summary = "Logout user", description = "Logs out the currently authenticated user.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Logout successful"),
            @ApiResponse(responseCode = "403", description = "User not authenticated", content = @Content)
    })
    @PostMapping(ApiPath.USER_LOGOUT)
    public void logout() {
        userService.invalidateToken();
    }

    @Operation(summary = "Get authenticated user", description = "Returns the currently authenticated user.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Authenticated user retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "User not authenticated", content = @Content)
    })
    @GetMapping(ApiPath.USER_ME)
    public ResponseEntity<UserDto> authenticatedUser() {
        var userOpt = userService.getAuthenticatedUser();
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(userOpt.get());
    }

    @Operation(summary = "Update personal info", description = "Updates the authenticated user's personal information.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User updated successfully"),
            @ApiResponse(responseCode = "403", description = "User not authenticated", content = @Content)
    })
    @PutMapping(ApiPath.USER_UPDATE_INFO)
    public ResponseEntity<UserDto> updateAuthenticatedUserInfo(@Valid @RequestBody UserUpdateInfoRequestDto request) {
        var userOpt = userService.updateAuthenticatedUserInfo(request);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(userOpt.get());
    }

    @Operation(summary = "Change password", description = "Changes the authenticated user's password.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Password changed successfully"),
            @ApiResponse(responseCode = "400", description = "Old password does not match", content = @Content),
            @ApiResponse(responseCode = "403", description = "User not authenticated", content = @Content)
    })
    @PutMapping(ApiPath.USER_UPDATE_PASSWORD)
    public ResponseEntity<Void> changeAuthenticatedUserPassword(@Valid @RequestBody UserChangePasswordRequestDto request) {
        Boolean result = userService.changeAuthenticatedUserPassword(request);
        if (result == null) {
            return ResponseEntity.status(401).build();
        }
        if (!result) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok().build();
    }
}
