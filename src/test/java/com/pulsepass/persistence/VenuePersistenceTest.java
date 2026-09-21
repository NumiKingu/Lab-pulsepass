package com.pulsepass.persistence;

import com.pulsepass.domain.Venue;
import com.pulsepass.repository.VenueRepository;
import com.pulsepass.support.AbstractPostgresIT;
import com.pulsepass.support.TestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


class VenuePersistenceTest extends AbstractPostgresIT {

    @Autowired
    private VenueRepository venueRepository;

    @Test
    @DisplayName("AC-001 / FR-VEN-001: un venue valido se recupera por ID y por codigo")
    void persistsAndFindsVenueByIdAndCode() {
        Venue saved = venueRepository.saveAndFlush(TestData.marinaVenue());

        assertThat(venueRepository.findById(saved.getId())).isPresent();

        Venue found = venueRepository.findByCode("VEN-SMR-01").orElseThrow();
        assertThat(found.getName()).isEqualTo("Marina Convention Center");
        assertThat(found.getCity()).isEqualTo("Santa Marta");
        assertThat(found.getCapacity()).isEqualTo(5000).isPositive();
        assertThat(found.isActive()).isTrue();
    }

    @Test
    @DisplayName("QT-009 / FR-VEN-002: PostgreSQL rechaza un codigo de venue duplicado")
    void rejectsDuplicateVenueCode() {
        venueRepository.saveAndFlush(TestData.marinaVenue());

        assertThatThrownBy(() -> venueRepository.saveAndFlush(TestData.marinaVenue()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("FR-VEN-003: PostgreSQL rechaza capacidad igual a cero")
    void rejectsZeroCapacity() {
        Venue invalid = new Venue("VEN-BAD-01", "Invalido", "Bogota", "Calle 0", 0);

        assertThatThrownBy(() -> venueRepository.saveAndFlush(invalid))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("FR-VEN-003: PostgreSQL rechaza capacidad negativa")
    void rejectsNegativeCapacity() {
        Venue invalid = new Venue("VEN-BAD-02", "Invalido", "Bogota", "Calle 0", -10);

        assertThatThrownBy(() -> venueRepository.saveAndFlush(invalid))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
