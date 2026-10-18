package com.pulsepass.service;

import com.pulsepass.domain.EventStatus;
import com.pulsepass.domain.Role;
import com.pulsepass.domain.User;
import com.pulsepass.repository.TicketRepository;
import com.pulsepass.repository.UserRepository;
import com.pulsepass.security.SecurityPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;

@Service("authorizationService")
@Transactional(readOnly = true)
public class AuthorizationService {
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;

    public AuthorizationService(TicketRepository ticketRepository, UserRepository userRepository) {
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
    }

    public boolean isTicketOwner(String ticketCode) {
        Long currentUserId = currentUserId();
        if (currentUserId == null || ticketCode == null) {
            return false;
        }
        return ticketRepository.findOwnerIdByTicketCode(ticketCode)
                .map(ownerId -> ownerId.equals(currentUserId))
                .orElse(false);
    }

    public boolean isCurrentUserEmail(String email) {
        Long currentUserId = currentUserId();
        if (currentUserId == null || email == null) {
            return false;
        }
        return userRepository.findByEmailIgnoreCase(email.trim())
                .map(User::getId)
                .map(id -> id.equals(currentUserId))
                .orElse(false);
    }

    public boolean isCurrentUsername(String username) {
        Long currentUserId = currentUserId();
        if (currentUserId == null || username == null) {
            return false;
        }
        return userRepository.findByUsername(username.trim())
                .map(User::getId)
                .map(id -> id.equals(currentUserId))
                .orElse(false);
    }

    public boolean canViewEventStatus(EventStatus status) {
        return status == EventStatus.PUBLISHED || hasRole(Role.STAFF) || hasRole(Role.ADMIN);
    }

    public boolean hasRole(Role role) {
        return currentPrincipal().map(p -> p.hasRole(role)).orElse(false);
    }

    public Optional<SecurityPrincipal> currentPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        if (authentication.getPrincipal() instanceof SecurityPrincipal principal) {
            return Optional.of(principal);
        }
        return Optional.empty();
    }

    private Long currentUserId() {
        return currentPrincipal().map(SecurityPrincipal::getId).filter(Objects::nonNull).orElse(null);
    }
}
