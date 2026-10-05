package com.pulsepass.persistence;

import com.pulsepass.domain.User;
import com.pulsepass.domain.UserProfile;
import com.pulsepass.repository.UserProfileRepository;
import com.pulsepass.repository.UserRepository;
import com.pulsepass.support.AbstractPostgresIT;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


class UserProfileIT extends AbstractPostgresIT {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("FR-USR-001: se registra un usuario y se recupera")
    void persistsUser() {
        User saved = userRepository.saveAndFlush(new User("andrea", "andrea@pulsepass.test"));

        User found = userRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.getUsername()).isEqualTo("andrea");
        assertThat(found.isActive()).isTrue();
    }

    @Test
    @DisplayName("FR-USR-001: busqueda por email ignorando mayusculas")
    void findsUserByEmailIgnoringCase() {
        userRepository.saveAndFlush(new User("andrea", "andrea@pulsepass.test"));

        assertThat(userRepository.findByEmailIgnoreCase("ANDREA@PulsePass.TEST")).isPresent();
    }

    @Test
    @DisplayName("QT-004 / FR-USR-004 / UC-04: usuario y perfil se persisten y se recuperan (1:1)")
    void persistsUserWithProfile() {
        User user = new User("andrea", "andrea@pulsepass.test");
        UserProfile profile = new UserProfile("Andrea", "Gomez");
        profile.setPhone("3001234567");
        profile.setCity("Santa Marta");
        profile.setBirthDate(LocalDate.of(1998, 5, 20));
        user.setProfile(profile);
        userRepository.saveAndFlush(user);
        entityManager.clear();

        User reloaded = userRepository.findByUsername("andrea").orElseThrow();

        assertThat(reloaded.getProfile()).isNotNull();
        assertThat(reloaded.getProfile().getFirstName()).isEqualTo("Andrea");
        assertThat(reloaded.getProfile().getCity()).isEqualTo("Santa Marta");
        assertThat(reloaded.getProfile().getBirthDate()).isEqualTo(LocalDate.of(1998, 5, 20));
        assertThat(reloaded.getProfile().getUser().getEmail()).isEqualTo("andrea@pulsepass.test");
    }

    @Test
    @DisplayName("AC-004 / FR-USR-003: la base impide un segundo perfil para el mismo usuario")
    void rejectsSecondProfileForSameUser() {
        User user = userRepository.saveAndFlush(new User("andrea", "andrea@pulsepass.test"));

        UserProfile first = new UserProfile("Andrea", "Gomez");
        first.setUser(user);
        userProfileRepository.saveAndFlush(first);

        UserProfile second = new UserProfile("Andrea", "Otra");
        second.setUser(user);

        assertThatThrownBy(() -> userProfileRepository.saveAndFlush(second))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("QT-009 / FR-USR-002: PostgreSQL rechaza un username duplicado")
    void rejectsDuplicateUsername() {
        userRepository.saveAndFlush(new User("andrea", "andrea@pulsepass.test"));

        assertThatThrownBy(() -> userRepository.saveAndFlush(new User("andrea", "otro@pulsepass.test")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("FR-USR-002: PostgreSQL rechaza un email duplicado")
    void rejectsDuplicateEmail() {
        userRepository.saveAndFlush(new User("andrea", "andrea@pulsepass.test"));

        assertThatThrownBy(() -> userRepository.saveAndFlush(new User("andrea2", "andrea@pulsepass.test")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
