package org.dreamabout.sw.frp.be.module.common.service;

import lombok.RequiredArgsConstructor;
import org.dreamabout.sw.frp.be.domain.exception.UserAlreadyExistsException;
import org.dreamabout.sw.frp.be.module.common.model.UserEntity;
import org.dreamabout.sw.frp.be.module.common.model.dto.UserDto;
import org.dreamabout.sw.frp.be.module.common.model.dto.UserLoginRequestDto;
import org.dreamabout.sw.frp.be.module.common.model.dto.UserLoginResponseDto;
import org.dreamabout.sw.frp.be.module.common.model.dto.UserRegisterRequestDto;
import org.dreamabout.sw.frp.be.module.common.model.dto.UserUpdateRequestDto;
import org.dreamabout.sw.frp.be.module.common.model.mapper.UserMapper;
import org.dreamabout.sw.frp.be.module.common.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public UserDto signup(UserRegisterRequestDto userRegister) {
        if (userRepository.findByEmail(userRegister.email()).isPresent()) {
            throw new UserAlreadyExistsException(userRegister.email());
        }
        var user = UserEntity.builder()
                .email(userRegister.email())
                .password(passwordEncoder.encode(userRegister.password()))
                .fullName(userRegister.fullName())
                .build();
        user = userRepository.save(user);
        return userMapper.toDto(user);
    }

    @Transactional(readOnly = true)
    public UserLoginResponseDto authenticate(UserLoginRequestDto userLogin) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        userLogin.email(),
                        userLogin.password()
                )
        );

        var user = userRepository.findByEmail(userLogin.email())
                .orElseThrow();

        var token = jwtService.generateToken(user);
        return new UserLoginResponseDto(token, userMapper.toDto(user));
    }

    @Transactional(readOnly = true)
    public Optional<UserDto> getAuthenticatedUser() {
        var aut = SecurityContextHolder.getContext().getAuthentication();
        if (aut == null || aut.getPrincipal() == null) {
            return Optional.empty();
        }
        var user = (UserEntity) aut.getPrincipal();
        return userRepository.findById(user.getId())
                .map(userMapper::toDto);
    }

    public Optional<UserDto> updateAuthenticatedUser(UserUpdateRequestDto update) {
        var aut = SecurityContextHolder.getContext().getAuthentication();
        if (aut == null || aut.getPrincipal() == null) {
            return Optional.empty();
        }
        var principal = (UserEntity) aut.getPrincipal();
        var user = userRepository.findById(principal.getId()).orElseThrow();
        user.setFullName(update.fullName());
        user.setEmail(update.email());
        user.setPassword(passwordEncoder.encode(update.password()));
        user = userRepository.save(user);
        return Optional.of(userMapper.toDto(user));
    }
}
