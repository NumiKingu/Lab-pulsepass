package com.pulsepass.security;

import com.pulsepass.domain.Role;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

public class SecurityPrincipal implements UserDetails, CredentialsContainer {

    private final Long id;
    private final String username;
    private String passwordHash;
    private final boolean active;
    private final Set<Role> roles;
    private final Set<GrantedAuthority> authorities;

    public SecurityPrincipal(Long id, String username, String passwordHash, boolean active, Set<Role> roles) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.active = active;
        this.roles = Set.copyOf(roles);
        this.authorities = roles.stream()
                .map(role -> new SimpleGrantedAuthority(role.authority()))
                .collect(Collectors.toUnmodifiableSet());
    }

    public Long getId() { return id; }

    public Set<Role> getRoles() { return roles; }

    public boolean hasRole(Role role) { return roles.contains(role); }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() { return authorities; }

    @Override
    public String getPassword() { return passwordHash; }

    @Override
    public String getUsername() { return username; }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return active; }

    /** Spring Security la llama tras autenticar: el hash no queda en memoria del principal. */
    @Override
    public void eraseCredentials() { this.passwordHash = null; }

    @Override
    public String toString() {
        return "SecurityPrincipal{id=" + id + ", username='" + username + "', roles=" + roles + "}";
    }
}
