package org.dreamabout.sw.frp.be.module.common.service;

import lombok.RequiredArgsConstructor;
import org.dreamabout.sw.frp.be.domain.exception.UserAlreadyExistsException;
import org.dreamabout.sw.frp.be.module.common.model.UserEntity;
import org.dreamabout.sw.frp.be.module.common.model.dto.*;
import org.dreamabout.sw.frp.be.module.common.model.mapper.UserMapper;
import org.dreamabout.sw.frp.be.module.common.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final SchemaService schemaService;

    public UserDto signup(UserRegisterRequestDto userRegister) {
        if (userRepository.findByEmail(userRegister.email()).isPresent()) {
            throw new UserAlreadyExistsException(userRegister.email());
        }

        String schemaName = userRegister.schemaName();
        if (schemaName == null || schemaName.isBlank()) {
            String emailPrefix = userRegister.email().split("@")[0];
            schemaName = sanitizeSchemaName(emailPrefix) + "_schema";
        }

        var schema = schemaService.createSchema(schemaName);

        var user = UserEntity.builder()
                .email(userRegister.email())
                .password(passwordEncoder.encode(userRegister.password()))
                .fullName(userRegister.fullName())
                .schema(schema)
                .build();
        user = userRepository.save(user);
        return userMapper.toDto(user);
    }

    private String sanitizeSchemaName(String name) {
        String sanitized = name.replaceAll("[^a-zA-Z0-9_]", "").toLowerCase();
        if (sanitized.isEmpty() || !Character.isLetter(sanitized.charAt(0))) {
            return "u_" + sanitized;
        }
        return sanitized;
    }

    public UserLoginResponseDto authenticate(UserLoginRequestDto userLogin) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        userLogin.email(),
                        userLogin.password()
                )
        );
        var user = userRepository.findByEmail(userLogin.email())
                .orElseThrow();
        user.setLastLogin(Instant.now());
        user.setTokenValid(true);
        var token = jwtService.generateToken(user);
        return new UserLoginResponseDto(token, userMapper.toDto(user));
    }

    @Transactional(readOnly = true)
    public Optional<UserDto> getAuthenticatedUser() {
        var user = getCurrentUser();
        return user.map(userMapper::toDto);
    }

    public Optional<UserDto> updateAuthenticatedUserInfo(UserUpdateInfoRequestDto update) {
        var aut = SecurityContextHolder.getContext().getAuthentication();
        if (aut == null || aut.getPrincipal() == null) {
            return Optional.empty();
        }
        var principal = (UserEntity) aut.getPrincipal();
        var user = userRepository.findById(principal.getId()).orElseThrow();
        user.setFullName(update.fullName());
        user.setEmail(update.email());
        user = userRepository.save(user);
        return Optional.of(userMapper.toDto(user));
    }

    public Optional<Boolean> changeAuthenticatedUserPassword(UserChangePasswordRequestDto update) {
        var aut = SecurityContextHolder.getContext().getAuthentication();
        if (aut == null || aut.getPrincipal() == null) {
            return Optional.empty();
        }
        var principal = (UserEntity) aut.getPrincipal();
        var user = userRepository.findById(principal.getId()).orElseThrow();
        if (!passwordEncoder.matches(update.oldPassword(), user.getPassword())) {
            return Optional.of(false);
        }
        user.setPassword(passwordEncoder.encode(update.newPassword()));
        userRepository.save(user);
        return Optional.of(true);
    }

    public void invalidateToken() {
        var user = getCurrentUser();
        user.ifPresent(u -> {
            u.setTokenValid(false);
            userRepository.save(u);
        });
        SecurityContextHolder.clearContext();
    }

    private Optional<UserEntity> getCurrentUser() {
        var aut = SecurityContextHolder.getContext().getAuthentication();
        if (aut == null || aut.getPrincipal() == null) {
            return Optional.empty();
        }
        var principal = (UserEntity) aut.getPrincipal();
        return userRepository.findById(principal.getId());
    }
}
