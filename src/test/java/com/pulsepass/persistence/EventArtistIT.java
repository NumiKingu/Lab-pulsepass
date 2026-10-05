package com.pulsepass.persistence;

import com.pulsepass.domain.Artist;
import com.pulsepass.domain.Event;
import com.pulsepass.domain.EventStatus;
import com.pulsepass.domain.Venue;
import com.pulsepass.repository.ArtistRepository;
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


class EventArtistIT extends AbstractPostgresIT {

    @Autowired
    private VenueRepository venueRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private ArtistRepository artistRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbc;

    private Venue venue() {
        return venueRepository.save(TestData.marinaVenue());
    }

    private Artist artist(String stageName) {
        return artistRepository.findByStageName(stageName).orElseThrow();
    }

    @Test
    @DisplayName("FR-ART-001: el catalogo inicial (V2) se recupera por stageName")
    void findsArtistFromInitialCatalog() {
        Artist solarBeat = artist("Solar Beat");

        assertThat(solarBeat.getCountry()).isEqualTo("Colombia");
        assertThat(solarBeat.isActive()).isTrue();
    }

    @Test
    @DisplayName("FR-ART-001: se puede registrar un artista nuevo")
    void persistsNewArtist() {
        Artist saved = artistRepository.saveAndFlush(new Artist("Los Faroles", "Colombia", "Vallenato"));

        assertThat(artistRepository.findById(saved.getId())).isPresent();
    }

    @Test
    @DisplayName("QT-009 / FR-ART-002: PostgreSQL rechaza un stageName duplicado")
    void rejectsDuplicateStageName() {
        assertThatThrownBy(() -> artistRepository.saveAndFlush(new Artist("Solar Beat", "Peru", "Rock")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("AC-003 / QT-005: un evento con tres artistas los relaciona sin duplicar pares")
    void eventWithThreeArtists() {
        Event event = TestData.caribbeanMusicFest(venue());
        event.addArtist(artist("Solar Beat"));
        event.addArtist(artist("Neon Waves"));
        event.addArtist(artist("Caribbean Sound"));
        event.addArtist(artist("Solar Beat")); // repetido: el Set no lo duplica
        eventRepository.saveAndFlush(event);
        entityManager.clear();

        Event reloaded = eventRepository.findByEventCode("CMF-2026").orElseThrow();

        assertThat(reloaded.getArtists()).extracting(Artist::getStageName)
                .containsExactlyInAnyOrder("Solar Beat", "Neon Waves", "Caribbean Sound");
        assertThat(jdbc.queryForObject("select count(*) from event_artists", Integer.class)).isEqualTo(3);
    }

    @Test
    @DisplayName("AC-003: la base de datos rechaza el mismo par evento-artista (PK compuesta)")
    void rejectsDuplicateEventArtistPair() {
        Event event = TestData.caribbeanMusicFest(venue());
        event.addArtist(artist("Solar Beat"));
        eventRepository.saveAndFlush(event);
        Long artistId = artist("Solar Beat").getId();

        assertThatThrownBy(() -> jdbc.update(
                "insert into event_artists (event_id, artist_id) values (?, ?)", event.getId(), artistId))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("QT-005: quitar un artista elimina solo esa asociacion")
    void removesArtistAssociation() {
        Event event = TestData.caribbeanMusicFest(venue());
        event.addArtist(artist("Solar Beat"));
        event.addArtist(artist("Neon Waves"));
        eventRepository.saveAndFlush(event);

        event.removeArtist(artist("Neon Waves"));
        eventRepository.saveAndFlush(event);
        entityManager.clear();

        Event reloaded = eventRepository.findByEventCode("CMF-2026").orElseThrow();
        assertThat(reloaded.getArtists()).extracting(Artist::getStageName).containsExactly("Solar Beat");
        assertThat(artistRepository.findByStageName("Neon Waves")).isPresent(); // el artista sigue existiendo
    }

    @Test
    @DisplayName("AC-007 / FR-ART-004 / FR-SRC-001: eventos de Solar Beat aparecen una sola vez")
    void findsEventsByArtistWithoutDuplicates() {
        Venue venue = venue();
        Artist solarBeat = artist("Solar Beat");
        Artist neonWaves = artist("Neon Waves");

        Event first = TestData.event("EVT-1", EventStatus.PUBLISHED, LocalDateTime.of(2026, 11, 1, 20, 0), venue);
        first.addArtist(solarBeat);
        first.addArtist(neonWaves);
        Event second = TestData.event("EVT-2", EventStatus.PUBLISHED, LocalDateTime.of(2026, 12, 1, 20, 0), venue);
        second.addArtist(solarBeat);
        Event third = TestData.event("EVT-3", EventStatus.DRAFT, LocalDateTime.of(2027, 1, 1, 20, 0), venue);
        third.addArtist(solarBeat);
        Event unrelated = TestData.event("EVT-4", EventStatus.PUBLISHED, LocalDateTime.of(2026, 11, 15, 20, 0), venue);
        unrelated.addArtist(neonWaves);
        eventRepository.saveAll(List.of(first, second, third, unrelated));
        eventRepository.flush();
        entityManager.clear();

        List<Event> result = eventRepository.findByArtistStageName("Solar Beat");

        assertThat(result).extracting(Event::getEventCode).containsExactly("EVT-1", "EVT-2", "EVT-3");
    }

    @Test
    @DisplayName("FR-ART-004: un artista puede navegar hacia sus eventos (lado inverso)")
    void artistSeesItsEvents() {
        Venue venue = venue();
        Artist oceanDrive = artist("Ocean Drive");
        Event first = TestData.event("EVT-1", EventStatus.PUBLISHED, LocalDateTime.of(2026, 11, 1, 20, 0), venue);
        first.addArtist(oceanDrive);
        Event second = TestData.event("EVT-2", EventStatus.PUBLISHED, LocalDateTime.of(2026, 12, 1, 20, 0), venue);
        second.addArtist(oceanDrive);
        eventRepository.saveAll(List.of(first, second));
        eventRepository.flush();
        entityManager.clear();

        Artist reloaded = artist("Ocean Drive");

        assertThat(reloaded.getEvents()).extracting(Event::getEventCode).containsExactlyInAnyOrder("EVT-1", "EVT-2");
    }
}
