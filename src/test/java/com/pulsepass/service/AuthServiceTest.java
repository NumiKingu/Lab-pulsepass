package com.pulsepass.service;

import com.pulsepass.domain.Role;
import com.pulsepass.domain.User;
import com.pulsepass.domain.UserCredential;
import com.pulsepass.domain.UserRole;
import com.pulsepass.dto.request.LoginRequest;
import com.pulsepass.dto.request.RegisterUserRequest;
import com.pulsepass.dto.response.AuthResponse;
import com.pulsepass.dto.response.UserResponse;
import com.pulsepass.exception.DuplicateResourceException;
import com.pulsepass.repository.UserCredentialRepository;
import com.pulsepass.repository.UserRepository;
import com.pulsepass.repository.UserRoleRepository;
import com.pulsepass.security.JwtService;
import com.pulsepass.security.SecurityPrincipal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    private static final String PASSWORD = "UnaFraseSeguraLarga2026!";

    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtService jwtService;
    @Mock private UserService userService;
    @Mock private UserRepository userRepository;
    @Mock private UserCredentialRepository credentialRepository;
    @Mock private UserRoleRepository roleRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @InjectMocks private AuthService authService;

    @Test
    @DisplayName("Login correcto -> token Bearer con expiresIn 900, emitido SOLO tras autenticar")
    void loginCorrecto() {
        SecurityPrincipal andrea = new SecurityPrincipal(1L, "andrea", null, true, Set.of(Role.USER));
        when(authenticationManager.authenticate(any()))
                .thenReturn(UsernamePasswordAuthenticationToken.authenticated(andrea, null, andrea.getAuthorities()));
        when(jwtService.generateToken(1L)).thenReturn("jwt-firmado");
        when(jwtService.getAccessTokenTtlSeconds()).thenReturn(900L);

        AuthResponse response = authService.login(new LoginRequest("andrea", PASSWORD));

        assertThat(response).isEqualTo(new AuthResponse("jwt-firmado", "Bearer", 900L));
    }

    @Test
    @DisplayName("Contraseña incorrecta -> BadCredentialsException y no se emite token")
    void loginPasswordIncorrecta() {
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(new LoginRequest("andrea", "incorrecta-123")))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid credentials");
        verify(jwtService, never()).generateToken(anyLong());
    }

    @Test
    @DisplayName("Usuario inactivo -> mismo error generico (no revela el estado)")
    void loginUsuarioInactivo() {
        when(authenticationManager.authenticate(any())).thenThrow(new DisabledException("User is disabled"));

        assertThatThrownBy(() -> authService.login(new LoginRequest("andrea", PASSWORD)))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid credentials");
        verify(jwtService, never()).generateToken(anyLong());
    }

    @Test
    @DisplayName("Registro guarda hash BCrypt (no la contraseña) y rol USER")
    void registroGuardaHashYRolUser() {
        UserResponse creado = new UserResponse(1L, "andrea", "andrea@pulsepass.com", true,
                "Andrea", "Perez", null, "Santa Marta", LocalDate.of(1998, 5, 10));
        User andrea = new User("andrea", "andrea@pulsepass.com");
        ReflectionTestUtils.setField(andrea, "id", 1L);

        when(userService.register(any(RegisterUserRequest.class))).thenReturn(creado);
        when(userRepository.getReferenceById(1L)).thenReturn(andrea);
        when(passwordEncoder.encode(PASSWORD)).thenReturn("$2a$12$hashDePrueba");

        UserResponse response = authService.register(solicitud());

        assertThat(response).isEqualTo(creado);
        ArgumentCaptor<UserCredential> credencial = ArgumentCaptor.forClass(UserCredential.class);
        verify(credentialRepository).save(credencial.capture());
        assertThat(credencial.getValue().getPasswordHash()).isEqualTo("$2a$12$hashDePrueba");

        ArgumentCaptor<UserRole> rol = ArgumentCaptor.forClass(UserRole.class);
        verify(roleRepository).save(rol.capture());
        assertThat(rol.getValue().getRole()).isEqualTo(Role.USER);
    }

    @Test
    @DisplayName("Si UserService rechaza el registro, no se crea credencial ni rol")
    void registroDuplicadoNoCreaCredencial() {
        when(userService.register(any(RegisterUserRequest.class)))
                .thenThrow(new DuplicateResourceException("Username already exists: andrea"));

        assertThatThrownBy(() -> authService.register(solicitud())).isInstanceOf(DuplicateResourceException.class);
        verify(credentialRepository, never()).save(any());
        verify(roleRepository, never()).save(any());
    }

    private static RegisterUserRequest solicitud() {
        return new RegisterUserRequest("andrea", "andrea@pulsepass.com", "Andrea", "Perez",
                null, "Santa Marta", LocalDate.of(1998, 5, 10), PASSWORD);
    }
}
