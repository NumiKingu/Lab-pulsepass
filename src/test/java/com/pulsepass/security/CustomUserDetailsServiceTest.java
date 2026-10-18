package com.pulsepass.security;

import com.pulsepass.domain.Role;
import com.pulsepass.domain.User;
import com.pulsepass.repository.UserCredentialRepository;
import com.pulsepass.repository.UserRepository;
import com.pulsepass.repository.UserRoleRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {
    @Mock private UserRepository userRepository;
    @Mock private UserCredentialRepository credentialRepository;
    @Mock private UserRoleRepository roleRepository;
    @InjectMocks private CustomUserDetailsService service;

    @Test
    @DisplayName("Carga por username con hash, roles y estado")
    void cargaPorUsername() {
        User andrea = usuario(1L, "andrea", true);
        when(userRepository.findByUsername("andrea")).thenReturn(Optional.of(andrea));
        when(credentialRepository.findPasswordHashByUserId(1L)).thenReturn(Optional.of("$2a$12$hash"));
        when(roleRepository.findRolesByUserId(1L)).thenReturn(Set.of(Role.USER));

        SecurityPrincipal principal = service.loadUserByUsername("andrea");

        assertThat(principal.getId()).isEqualTo(1L);
        assertThat(principal.getPassword()).isEqualTo("$2a$12$hash");
        assertThat(principal.isEnabled()).isTrue();
        assertThat(principal.getAuthorities()).extracting("authority").containsExactly("ROLE_USER");
    }

    @Test
    @DisplayName("Carga por email (ignora mayusculas)")
    void cargaPorEmail() {
        User andrea = usuario(1L, "andrea", true);
        when(userRepository.findByEmailIgnoreCase("Andrea@PulsePass.com")).thenReturn(Optional.of(andrea));
        when(credentialRepository.findPasswordHashByUserId(1L)).thenReturn(Optional.of("$2a$12$hash"));
        when(roleRepository.findRolesByUserId(1L)).thenReturn(Set.of(Role.USER));

        assertThat(service.loadUserByUsername("Andrea@PulsePass.com").getUsername()).isEqualTo("andrea");
    }

    @Test
    @DisplayName("Usuario inexistente o legado sin credencial -> UsernameNotFoundException")
    void inexistenteOSinCredencial() {
        when(userRepository.findByUsername("nadie")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.loadUserByUsername("nadie"))
                .isInstanceOf(UsernameNotFoundException.class);

        User legado = usuario(2L, "legado", true);
        when(userRepository.findByUsername("legado")).thenReturn(Optional.of(legado));
        when(credentialRepository.findPasswordHashByUserId(2L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.loadUserByUsername("legado"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    @DisplayName("Por ID: refleja estado inactivo y roles ACTUALES de la BD")
    void cargaPorIdConEstadoYRolesActuales() {
        User laura = usuario(3L, "laura", false);
        when(userRepository.findById(3L)).thenReturn(Optional.of(laura));
        when(roleRepository.findRolesByUserId(3L)).thenReturn(Set.of(Role.STAFF));

        SecurityPrincipal principal = service.loadUserById(3L);

        assertThat(principal.isEnabled()).isFalse();
        assertThat(principal.hasRole(Role.STAFF)).isTrue();
        assertThat(principal.getPassword()).isNull();
    }

    private static User usuario(Long id, String username, boolean active) {
        User user = new User(username, username + "@pulsepass.com");
        ReflectionTestUtils.setField(user, "id", id);
        user.setActive(active);
        return user;
    }
}
