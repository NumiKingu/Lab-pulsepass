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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


class EventSearchIT extends AbstractPostgresIT {

    private static final LocalDateTime AFTER = LocalDateTime.of(2026, 10, 1, 0, 0);

    @Autowired
    private VenueRepository venueRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private ArtistRepository artistRepository;

    @Autowired
    private EntityManager entityManager;


    @BeforeEach
    void setUp() {
        Venue santaMarta = venueRepository.save(TestData.marinaVenue());
        Venue bogota = venueRepository.save(TestData.venue("VEN-BOG-01", "Bogota"));

        Artist solarBeat = artistRepository.findByStageName("Solar Beat").orElseThrow();
        Artist neonWaves = artistRepository.findByStageName("Neon Waves").orElseThrow();
        Artist solarWind = artistRepository.save(new Artist("Solar Wind", "Chile", "Rock"));

        Event smr1 = TestData.event("SMR-1", EventStatus.PUBLISHED, LocalDateTime.of(2026, 11, 1, 20, 0), santaMarta);
        smr1.addArtist(solarBeat);
        smr1.addArtist(solarWind);
        Event smr2 = TestData.event("SMR-2", EventStatus.PUBLISHED, LocalDateTime.of(2026, 12, 1, 20, 0), santaMarta);
        smr2.addArtist(neonWaves);
        Event smr3 = TestData.event("SMR-3", EventStatus.DRAFT, LocalDateTime.of(2026, 11, 15, 20, 0), santaMarta);
        smr3.addArtist(solarBeat);
        Event smr4 = TestData.event("SMR-4", EventStatus.PUBLISHED, LocalDateTime.of(2026, 9, 1, 20, 0), santaMarta);
        smr4.addArtist(solarBeat);
        Event bog1 = TestData.event("BOG-1", EventStatus.PUBLISHED, LocalDateTime.of(2026, 11, 10, 20, 0), bogota);
        bog1.addArtist(solarBeat);

        eventRepository.saveAll(List.of(smr1, smr2, smr3, smr4, bog1));
        eventRepository.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("FR-SRC-001: busca eventos por artista sin duplicados y en orden cronologico")
    void searchesEventsByArtist() {
        List<Event> result = eventRepository.findByArtistStageName("Solar Beat");

        assertThat(result).extracting(Event::getEventCode)
                .containsExactly("SMR-4", "SMR-1", "BOG-1", "SMR-3");
    }

    @Test
    @DisplayName("FR-SRC-002: filtra por venue.city y artist.stageName")
    void searchesEventsByCityAndArtist() {
        List<Event> result = eventRepository.findByCityAndArtistStageName("Santa Marta", "Solar Beat");

        assertThat(result).extracting(Event::getEventCode).containsExactly("SMR-4", "SMR-1", "SMR-3");
    }

    @Test
    @DisplayName("FR-SRC-003: recomendados = PUBLISHED, posteriores a la fecha, en la ciudad y con artista que contiene el texto")
    void recommendsPublishedFutureEventsInCity() {
        List<Event> result = eventRepository.findRecommended(AFTER, "Santa Marta", "Solar Beat");

        // SMR-3 (DRAFT), SMR-4 (fecha pasada) y BOG-1 (otra ciudad) quedan fuera
        assertThat(result).extracting(Event::getEventCode).containsExactly("SMR-1");
    }

    @Test
    @DisplayName("FR-SRC-003: la busqueda del artista es case-insensitive y por texto parcial")
    void recommendationIsCaseInsensitiveAndPartial() {
        List<Event> result = eventRepository.findRecommended(AFTER, "santa marta", "wAvEs");

        assertThat(result).extracting(Event::getEventCode).containsExactly("SMR-2");
    }

    @Test
    @DisplayName("FR-SRC-003: DISTINCT evita repetir un evento cuando varios artistas coinciden")
    void recommendationDoesNotRepeatEvents() {

        List<Event> result = eventRepository.findRecommended(AFTER, "Santa Marta", "solar");

        assertThat(result).extracting(Event::getEventCode).containsExactly("SMR-1");
    }

    @Test
    @DisplayName("FR-SRC-003: los resultados quedan ordenados por fecha ascendente")
    void recommendationIsOrderedByDate() {

        List<Event> result = eventRepository.findRecommended(AFTER, "Santa Marta", "a");

        assertThat(result).extracting(Event::getEventCode).containsExactly("SMR-1", "SMR-2");
    }
}
