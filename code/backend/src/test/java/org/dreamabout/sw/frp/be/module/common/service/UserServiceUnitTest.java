package org.dreamabout.sw.frp.be.module.common.service;

import org.dreamabout.sw.frp.be.module.common.model.SchemaEntity;
import org.dreamabout.sw.frp.be.module.common.model.UserEntity;
import org.dreamabout.sw.frp.be.module.common.model.dto.UserDto;
import org.dreamabout.sw.frp.be.module.common.model.dto.UserRegisterRequestDto;
import org.dreamabout.sw.frp.be.module.common.model.mapper.UserMapper;
import org.dreamabout.sw.frp.be.module.common.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UserServiceUnitTest {

    @Mock
    private UserRepository userRepository;
    
    @Mock
    private UserMapper userMapper;
    
    @Mock
    private PasswordEncoder passwordEncoder;
    
    @Mock
    private SchemaService schemaService;

    @InjectMocks
    private UserService userService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void signup_shouldCreateSchemaAndUser() {
        // Given
        var req = new UserRegisterRequestDto("test@example.com", "password", "Test User", "custom_schema");
        var schema = new SchemaEntity();
        schema.setName("custom_schema");
        
        when(userRepository.findByEmail(req.email())).thenReturn(Optional.empty());
        when(schemaService.createSchema("custom_schema")).thenReturn(schema);
        when(passwordEncoder.encode(req.password())).thenReturn("encoded_pass");
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> {
            UserEntity u = invocation.getArgument(0);
            u.setId(1L);
            return u;
        });
        when(userMapper.toDto(any(UserEntity.class))).thenAnswer(invocation -> {
            UserEntity u = invocation.getArgument(0);
            return new UserDto(u.getId(), u.getEmail(), u.getFullName(), u.getSchema().getName());
        });

        // When
        UserDto result = userService.signup(req);

        // Then
        assertNotNull(result);
        assertEquals("custom_schema", result.activeSchema());
        verify(schemaService).createSchema("custom_schema");
        verify(userRepository).save(any(UserEntity.class));
    }

    @Test
    void signup_shouldDeriveSchemaName_whenNotProvided() {
        // Given
        var req = new UserRegisterRequestDto("test.user@example.com", "password", "Test User", null);
        var schema = new SchemaEntity();
        schema.setName("testuser_schema");
        
        when(userRepository.findByEmail(req.email())).thenReturn(Optional.empty());
        when(schemaService.createSchema("testuser_schema")).thenReturn(schema);
        when(passwordEncoder.encode(req.password())).thenReturn("encoded_pass");
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userMapper.toDto(any(UserEntity.class))).thenAnswer(invocation -> {
            UserEntity u = invocation.getArgument(0);
            return new UserDto(u.getId(), u.getEmail(), u.getFullName(), u.getSchema().getName());
        });

        // When
        UserDto result = userService.signup(req);

        // Then
        assertEquals("testuser_schema", result.activeSchema());
        verify(schemaService).createSchema("testuser_schema");
    }

    @Test
    void signup_shouldHandleNumericEmailPrefix() {
        // Given
        var req = new UserRegisterRequestDto("123user@example.com", "password", "Num User", null);
        var schema = new SchemaEntity();
        schema.setName("u_123user_schema");
        
        when(userRepository.findByEmail(req.email())).thenReturn(Optional.empty());
        when(schemaService.createSchema("u_123user_schema")).thenReturn(schema);
        when(passwordEncoder.encode(req.password())).thenReturn("encoded_pass");
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userMapper.toDto(any(UserEntity.class))).thenAnswer(invocation -> {
            UserEntity u = invocation.getArgument(0);
            return new UserDto(u.getId(), u.getEmail(), u.getFullName(), u.getSchema().getName());
        });

        // When
        UserDto result = userService.signup(req);

        // Then
        assertEquals("u_123user_schema", result.activeSchema());
        verify(schemaService).createSchema("u_123user_schema");
    }
}

