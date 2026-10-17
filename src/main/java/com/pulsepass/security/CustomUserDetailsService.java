package com.pulsepass.security;

import com.pulsepass.domain.Role;
import com.pulsepass.domain.User;
import com.pulsepass.repository.UserCredentialRepository;
import com.pulsepass.repository.UserRepository;
import com.pulsepass.repository.UserRoleRepository;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class CustomUserDetailsService implements UserDetailsService {

    private static final String GENERIC_MESSAGE = "Invalid credentials";

    private final UserRepository userRepository;
    private final UserCredentialRepository credentialRepository;
    private final UserRoleRepository roleRepository;

    public CustomUserDetailsService(UserRepository userRepository,
                                    UserCredentialRepository credentialRepository,
                                    UserRoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.credentialRepository = credentialRepository;
        this.roleRepository = roleRepository;
    }

    @Override
    public SecurityPrincipal loadUserByUsername(String identifier) {
        String value = identifier == null ? "" : identifier.trim();

        Optional<User> found = value.contains("@")
                ? userRepository.findByEmailIgnoreCase(value)
                : userRepository.findByUsername(value);

        User user = found.orElseThrow(() -> new UsernameNotFoundException(GENERIC_MESSAGE));

        String passwordHash = credentialRepository.findPasswordHashByUserId(user.getId())
                .orElseThrow(() -> new UsernameNotFoundException(GENERIC_MESSAGE));

        return toPrincipal(user, passwordHash);
    }

    public SecurityPrincipal loadUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException(GENERIC_MESSAGE));
        return toPrincipal(user, null);
    }

    private SecurityPrincipal toPrincipal(User user, String passwordHash) {
        Set<Role> roles = roleRepository.findRolesByUserId(user.getId());
        return new SecurityPrincipal(user.getId(), user.getUsername(), passwordHash, user.isActive(), roles);
    }
}
