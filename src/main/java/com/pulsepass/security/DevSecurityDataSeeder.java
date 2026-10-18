package com.pulsepass.security;

import com.pulsepass.domain.Role;
import com.pulsepass.domain.User;
import com.pulsepass.domain.UserCredential;
import com.pulsepass.domain.UserProfile;
import com.pulsepass.domain.UserRole;
import com.pulsepass.repository.UserCredentialRepository;
import com.pulsepass.repository.UserProfileRepository;
import com.pulsepass.repository.UserRepository;
import com.pulsepass.repository.UserRoleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Component
@Profile("dev")
public class DevSecurityDataSeeder implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(DevSecurityDataSeeder.class);

    private final UserRepository userRepository;
    private final UserProfileRepository profileRepository;
    private final UserCredentialRepository credentialRepository;
    private final UserRoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminPassword;
    private final String staffPassword;

    public DevSecurityDataSeeder(UserRepository userRepository,
                                 UserProfileRepository profileRepository,
                                 UserCredentialRepository credentialRepository,
                                 UserRoleRepository roleRepository,
                                 PasswordEncoder passwordEncoder,
                                 @Value("${PULSEPASS_SEED_ADMIN_PASSWORD:}") String adminPassword,
                                 @Value("${PULSEPASS_SEED_STAFF_PASSWORD:}") String staffPassword) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.credentialRepository = credentialRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminPassword = adminPassword;
        this.staffPassword = staffPassword;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seed("miguel", "miguel@pulsepass.local", "Miguel", "Admin", adminPassword, Role.ADMIN);
        seed("laura", "laura@pulsepass.local", "Laura", "Staff", staffPassword, Role.STAFF);
    }

    private void seed(String username, String email, String firstName, String lastName,
                      String rawPassword, Role role) {
        if (rawPassword == null || rawPassword.length() < 12) {
            log.info("Semilla {} omitida: defina su contraseña (>= 12 caracteres) por variable de entorno", role);
            return;
        }
        if (userRepository.existsByUsername(username)) {
            return;
        }
        User user = userRepository.save(new User(username, email));
        profileRepository.save(new UserProfile(user, firstName, lastName, null, "Santa Marta",
                LocalDate.of(1995, 1, 1)));
        credentialRepository.save(new UserCredential(user, passwordEncoder.encode(rawPassword)));
        roleRepository.save(new UserRole(user, role));
        log.info("Cuenta {} creada para entorno local: {}", role, username);
    }
}
