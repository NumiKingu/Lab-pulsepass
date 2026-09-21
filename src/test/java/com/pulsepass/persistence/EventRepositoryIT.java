package com.pulsepass.persistence;

import com.pulsepass.domain.Event;
import com.pulsepass.domain.EventStatus;
import com.pulsepass.domain.Venue;
import com.pulsepass.repository.EventRepository;
import com.pulsepass.repository.VenueRepository;
import com.pulsepass.support.AbstractPostgresIT;
import com.pulsepass.support.TestData;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


class EventRepositoryIT extends AbstractPostgresIT {

    @Autowired
    private VenueRepository venueRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    @DisplayName("AC-002 / QT-003: el evento CMF-2026 se recupera por eventCode junto con su venue")
    void findsEventByCodeWithItsVenue() {
        Venue venue = venueRepository.save(TestData.marinaVenue());
        eventRepository.saveAndFlush(TestData.caribbeanMusicFest(venue));
        entityManager.clear();

        Event found = eventRepository.findByEventCode("CMF-2026").orElseThrow();

        assertThat(found.getName()).isEqualTo("Caribbean Music Fest 2026");
        assertThat(found.getVenue().getCode()).isEqualTo("VEN-SMR-01");
    }

    @Test
    @DisplayName("QT-003 / FR-VEN-004: eventos por codigo de venue solo retorna los de ese venue")
    void findsEventsOnlyFromRequestedVenue() {
        Venue marina = venueRepository.save(TestData.marinaVenue());
        Venue other = venueRepository.save(TestData.venue("VEN-BOG-01", "Bogota"));
        eventRepository.save(TestData.event("EVT-A", EventStatus.PUBLISHED, LocalDateTime.of(2026, 11, 1, 20, 0), marina));
        eventRepository.save(TestData.event("EVT-B", EventStatus.DRAFT, LocalDateTime.of(2026, 11, 2, 20, 0), marina));
        eventRepository.saveAndFlush(TestData.event("EVT-C", EventStatus.PUBLISHED, LocalDateTime.of(2026, 11, 3, 20, 0), other));

        List<Event> result = eventRepository.findByVenueCode("VEN-SMR-01");

        assertThat(result).extracting(Event::getEventCode).containsExactlyInAnyOrder("EVT-A", "EVT-B");
    }

    @Test
    @DisplayName("QT-003: la relacion inversa Venue -> events refleja los eventos persistidos")
    void venueSeesItsEvents() {
        Venue venue = venueRepository.save(TestData.marinaVenue());
        eventRepository.save(TestData.event("EVT-A", EventStatus.PUBLISHED, LocalDateTime.of(2026, 11, 1, 20, 0), venue));
        eventRepository.saveAndFlush(TestData.event("EVT-B", EventStatus.PUBLISHED, LocalDateTime.of(2026, 11, 2, 20, 0), venue));
        entityManager.clear();

        Venue reloaded = venueRepository.findByCode("VEN-SMR-01").orElseThrow();

        assertThat(reloaded.getEvents()).extracting(Event::getEventCode).containsExactlyInAnyOrder("EVT-A", "EVT-B");
    }

    @Test
    @DisplayName("AC-006 / FR-EVT-005: solo eventos PUBLISHED, en orden cronologico ascendente")
    void returnsOnlyPublishedEventsOrderedByDate() {
        Venue venue = venueRepository.save(TestData.marinaVenue());
        eventRepository.save(TestData.event("EVT-LATE", EventStatus.PUBLISHED, LocalDateTime.of(2026, 12, 20, 20, 0), venue));
        eventRepository.save(TestData.event("EVT-DRAFT", EventStatus.DRAFT, LocalDateTime.of(2026, 10, 1, 20, 0), venue));
        eventRepository.save(TestData.event("EVT-CANCELLED", EventStatus.CANCELLED, LocalDateTime.of(2026, 10, 2, 20, 0), venue));
        eventRepository.saveAndFlush(TestData.event("EVT-EARLY", EventStatus.PUBLISHED, LocalDateTime.of(2026, 11, 5, 20, 0), venue));

        List<Event> result = eventRepository.findByStatusOrderByEventDateAsc(EventStatus.PUBLISHED);

        assertThat(result).extracting(Event::getEventCode).containsExactly("EVT-EARLY", "EVT-LATE");
    }

    @Test
    @DisplayName("QT-009 / FR-EVT-002: PostgreSQL rechaza un eventCode duplicado")
    void rejectsDuplicateEventCode() {
        Venue venue = venueRepository.save(TestData.marinaVenue());
        eventRepository.saveAndFlush(TestData.caribbeanMusicFest(venue));

        assertThatThrownBy(() -> eventRepository.saveAndFlush(TestData.caribbeanMusicFest(venue)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("FR-EVT-003: PostgreSQL rechaza un estado fuera del catalogo")
    void rejectsInvalidStatusAtDatabaseLevel() {
        Venue venue = venueRepository.saveAndFlush(TestData.marinaVenue());

        assertThatThrownBy(() -> jdbc.update("""
                insert into events (event_code, name, category, status, event_date, minimum_age, venue_id)
                values ('EVT-BAD', 'Malo', 'MUSIC', 'ARCHIVED', now(), 0, ?)
                """, venue.getId()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("FR-EVT-004 / BR-008: la categoria y el estado se guardan por nombre, no por ordinal")
    void storesEnumsByName() {
        Venue venue = venueRepository.save(TestData.marinaVenue());
        eventRepository.saveAndFlush(TestData.caribbeanMusicFest(venue));

        String category = jdbc.queryForObject("select category from events where event_code = 'CMF-2026'", String.class);
        String status = jdbc.queryForObject("select status from events where event_code = 'CMF-2026'", String.class);

        assertThat(category).isEqualTo("MUSIC");
        assertThat(status).isEqualTo("PUBLISHED");
    }

    @Test
    @DisplayName("FR-EVT-006: streamingUrl es opcional (NULL) y puede almacenarse")
    void streamingUrlIsOptional() {
        Venue venue = venueRepository.save(TestData.marinaVenue());
        Event onSite = eventRepository.save(TestData.caribbeanMusicFest(venue));
        Event hybrid = eventRepository.save(
                TestData.event("EVT-HYBRID", EventStatus.PUBLISHED, LocalDateTime.of(2026, 11, 10, 20, 0), venue));
        hybrid.setStreamingUrl("https://stream.pulsepass.test/evt-hybrid");
        entityManager.flush();
        entityManager.clear();

        assertThat(eventRepository.findById(onSite.getId()).orElseThrow().getStreamingUrl()).isNull();
        assertThat(eventRepository.findById(hybrid.getId()).orElseThrow().getStreamingUrl())
                .isEqualTo("https://stream.pulsepass.test/evt-hybrid");
    }

    @Test
    @DisplayName("BR-001: no puede existir un evento sin un venue existente (FK)")
    void rejectsEventWithoutValidVenue() {
        assertThatThrownBy(() -> jdbc.update("""
                insert into events (event_code, name, category, status, event_date, minimum_age, venue_id)
                values ('EVT-ORPHAN', 'Huerfano', 'MUSIC', 'DRAFT', now(), 0, 999999)
                """))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
