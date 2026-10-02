package org.dreamabout.sw.frp.be.module.common.service;

import lombok.RequiredArgsConstructor;
import org.dreamabout.sw.frp.be.config.security.SecurityContextService;
import org.dreamabout.sw.frp.be.domain.exception.UserAlreadyExistsException;
import org.dreamabout.sw.frp.be.module.common.domain.AuditAction;
import org.dreamabout.sw.frp.be.module.common.model.GroupEntity;
import org.dreamabout.sw.frp.be.module.common.model.UserEntity;
import org.dreamabout.sw.frp.be.module.common.model.dto.*;
import org.dreamabout.sw.frp.be.module.common.model.mapper.UserMapper;
import org.dreamabout.sw.frp.be.module.common.repository.GroupRepository;
import org.dreamabout.sw.frp.be.module.common.repository.UserRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final SchemaService schemaService;
    private final SecurityContextService securityContextService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public List<UserDto> getAllUsers() {
        return userRepository.findAll().stream()
                .map(userMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<UserDto> searchUsers(String query) {
        return userRepository.findByEmailContainingIgnoreCaseOrFullNameContainingIgnoreCase(query, query).stream()
                .map(userMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<UserDto> getUserById(Long id) {
        return userRepository.findById(id).map(userMapper::toDto);
    }

    public UserDto updateUserActiveStatus(Long id, boolean active) {
        var user = userRepository.findById(id).orElseThrow();
        user.setActive(active);
        audit(active ? AuditAction.USER_ACTIVATED : AuditAction.USER_DEACTIVATED, id, null);
        return userMapper.toDto(userRepository.save(user));
    }

    public UserDto updateUserAdminStatus(Long id, boolean admin) {
        var user = userRepository.findById(id).orElseThrow();
        user.setAdmin(admin);
        audit(admin ? AuditAction.ADMIN_GRANTED : AuditAction.ADMIN_REVOKED, id, null);
        return userMapper.toDto(userRepository.save(user));
    }

    public UserDto updateUserGroups(Long id, Set<Long> groupIds) {
        var user = userRepository.findById(id).orElseThrow();
        var groups = groupRepository.findAllById(groupIds);
        user.getGroups().clear();
        user.getGroups().addAll(groups);
        var groupNames = groups.stream().map(GroupEntity::getName).sorted().collect(Collectors.joining(","));
        audit(AuditAction.USER_GROUPS_CHANGED, id, "groups=" + groupNames);
        return userMapper.toDto(userRepository.save(user));
    }

    public UserDto signup(UserRegisterRequestDto userRegister) {
        if (userRepository.findByEmail(userRegister.email()).isPresent()) {
            throw new UserAlreadyExistsException(userRegister.email());
        }

        String schemaName = userRegister.schemaName();
        if (schemaName == null || schemaName.isBlank()) {
            String emailPrefix = userRegister.email().split("@")[0];
            schemaName = sanitizeSchemaName(emailPrefix) + "_schema";
        }

        var user = UserEntity.builder()
                .email(userRegister.email())
                .password(passwordEncoder.encode(userRegister.password()))
                .fullName(userRegister.fullName())
                .build();
        user = userRepository.save(user);

        var schema = schemaService.createSchema(schemaName, user.getId());
        
        user.setSchema(schema);
        user = userRepository.save(user);

        eventPublisher.publishEvent(AuditEvent.ofUser(AuditAction.USER_REGISTERED, user.getId(), user.getEmail(),
                "schema=" + schemaName));
        return userMapper.toDto(user);
    }

    private String sanitizeSchemaName(String name) {
        String sanitized = name.replaceAll("\\W", "").toLowerCase();
        if (sanitized.isEmpty() || !Character.isLetter(sanitized.charAt(0))) {
            return "u_" + sanitized;
        }
        return sanitized;
    }

    @Transactional(noRollbackFor = AuthenticationException.class)
    public UserLoginResponseDto authenticate(UserLoginRequestDto userLogin) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            userLogin.email(),
                            userLogin.password()
                    )
            );
        } catch (AuthenticationException e) {
            var userId = userRepository.findByEmail(userLogin.email()).map(UserEntity::getId).orElse(null);
            eventPublisher.publishEvent(AuditEvent.ofUser(AuditAction.LOGIN_FAILED, userId, userLogin.email(),
                    e.getClass().getSimpleName()));
            throw e;
        }
        var user = userRepository.findByEmail(userLogin.email())
                .orElseThrow();
        user.setLastLogin(Instant.now());
        user.setTokenValid(true);
        eventPublisher.publishEvent(AuditEvent.ofUser(AuditAction.LOGIN, user.getId(), user.getEmail(), null));
        var token = jwtService.generateToken(user);
        return new UserLoginResponseDto(token, userMapper.toDto(user));
    }

    @Transactional(readOnly = true)
    public Optional<UserDto> getAuthenticatedUser() {
        var user = getCurrentUser();
        return user.map(userMapper::toDto);
    }

    public Optional<UserDto> updateAuthenticatedUserInfo(UserUpdateInfoRequestDto update) {
        var aut = securityContextService.getAuthentication();
        if (aut != null && aut.getPrincipal() instanceof UserEntity principal) {
            var user = userRepository.findById(principal.getId()).orElseThrow();
            user.setFullName(update.fullName());
            user.setEmail(update.email());
            user = userRepository.save(user);
            eventPublisher.publishEvent(AuditEvent.ofUser(AuditAction.USER_INFO_CHANGED, user.getId(), user.getEmail(), null));
            return Optional.of(userMapper.toDto(user));
        }
        return Optional.empty();
    }

    public Optional<Boolean> changeAuthenticatedUserPassword(UserChangePasswordRequestDto update) {
        var aut = securityContextService.getAuthentication();
        if (aut != null && aut.getPrincipal() instanceof UserEntity principal) {
            var user = userRepository.findById(principal.getId()).orElseThrow();
            if (!passwordEncoder.matches(update.oldPassword(), user.getPassword())) {
                return Optional.of(false);
            }
            user.setPassword(passwordEncoder.encode(update.newPassword()));
            userRepository.save(user);
            eventPublisher.publishEvent(AuditEvent.ofUser(AuditAction.PASSWORD_CHANGED, user.getId(), user.getEmail(), null));
            return Optional.of(true);
        }
        return Optional.empty();
    }

    public void invalidateToken() {
        var user = getCurrentUser();
        user.ifPresent(u -> {
            u.setTokenValid(false);
            userRepository.save(u);
            eventPublisher.publishEvent(AuditEvent.ofUser(AuditAction.LOGOUT, u.getId(), u.getEmail(), null));
        });
        securityContextService.clearContext();
    }

    @Transactional(readOnly = true)
    public UserEntity getPrincipal() {
        return securityContextService.getPrincipal();
    }

    /**
     * Audits an administrative change of the target user; the acting admin is taken from the security context.
     */
    private void audit(AuditAction action, Long targetUserId, String details) {
        eventPublisher.publishEvent(AuditEvent.ofCurrentUser(action, AuditEvent.userResource(targetUserId), details));
    }

    private Optional<UserEntity> getCurrentUser() {
        var aut = securityContextService.getAuthentication();
        if (aut != null && aut.getPrincipal() instanceof UserEntity principal) {
            return userRepository.findById(principal.getId());
        }
        return Optional.empty();
    }
}
