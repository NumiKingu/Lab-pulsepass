package com.pulsepass.service;

import com.pulsepass.domain.EventStatus;
import com.pulsepass.domain.Role;
import com.pulsepass.domain.User;
import com.pulsepass.repository.TicketRepository;
import com.pulsepass.repository.UserRepository;
import com.pulsepass.security.SecurityPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthorizationServiceTest {
    @Mock private TicketRepository ticketRepository;
    @Mock private UserRepository userRepository;
    @InjectMocks private AuthorizationService authorizationService;

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Dueño del ticket -> true; tercero -> false (anti-IDOR)")
    void duenoVsTercero() {
        when(ticketRepository.findOwnerIdByTicketCode("TCK-CARLOS")).thenReturn(Optional.of(2L));

        autenticarComo(2L, Role.USER);
        assertThat(authorizationService.isTicketOwner("TCK-CARLOS")).isTrue();

        autenticarComo(1L, Role.USER);
        assertThat(authorizationService.isTicketOwner("TCK-CARLOS")).isFalse();
    }

    @Test
    @DisplayName("Ticket inexistente o usuario anonimo -> false")
    void inexistenteOAnonimo() {
        assertThat(authorizationService.isTicketOwner("TCK-X")).isFalse();

        autenticarComo(1L, Role.USER);
        when(ticketRepository.findOwnerIdByTicketCode("TCK-X")).thenReturn(Optional.empty());
        assertThat(authorizationService.isTicketOwner("TCK-X")).isFalse();
    }

    @Test
    @DisplayName("Email propio -> true; email ajeno -> false (compara IDs, no strings)")
    void emailPropioVsAjeno() {
        when(userRepository.findByEmailIgnoreCase("andrea@pulsepass.com")).thenReturn(Optional.of(usuario(1L, "andrea")));
        when(userRepository.findByEmailIgnoreCase("carlos@pulsepass.com")).thenReturn(Optional.of(usuario(2L, "carlos")));
        autenticarComo(1L, Role.USER);

        assertThat(authorizationService.isCurrentUserEmail("andrea@pulsepass.com")).isTrue();
        assertThat(authorizationService.isCurrentUserEmail("carlos@pulsepass.com")).isFalse();
    }

    @Test
    @DisplayName("Username propio -> true; ajeno -> false")
    void usernamePropioVsAjeno() {
        when(userRepository.findByUsername("andrea")).thenReturn(Optional.of(usuario(1L, "andrea")));
        when(userRepository.findByUsername("carlos")).thenReturn(Optional.of(usuario(2L, "carlos")));
        autenticarComo(1L, Role.USER);

        assertThat(authorizationService.isCurrentUsername("andrea")).isTrue();
        assertThat(authorizationService.isCurrentUsername("carlos")).isFalse();
    }

    @Test
    @DisplayName("Borradores: visibles solo para STAFF/ADMIN; publicados para todos")
    void visibilidadDeEventos() {
        assertThat(authorizationService.canViewEventStatus(EventStatus.PUBLISHED)).isTrue();
        assertThat(authorizationService.canViewEventStatus(EventStatus.DRAFT)).isFalse();

        autenticarComo(1L, Role.USER);
        assertThat(authorizationService.canViewEventStatus(EventStatus.DRAFT)).isFalse();

        autenticarComo(3L, Role.STAFF);
        assertThat(authorizationService.canViewEventStatus(EventStatus.DRAFT)).isTrue();

        autenticarComo(4L, Role.ADMIN);
        assertThat(authorizationService.canViewEventStatus(EventStatus.CANCELLED)).isTrue();
    }

    private static void autenticarComo(Long id, Role... roles) {
        SecurityPrincipal principal = new SecurityPrincipal(id, "u" + id, null, true, Set.of(roles));
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities()));
    }

    private static User usuario(Long id, String username) {
        User user = new User(username, username + "@pulsepass.com");
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
