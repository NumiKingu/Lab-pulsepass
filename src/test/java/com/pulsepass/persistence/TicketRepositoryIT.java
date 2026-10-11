package com.pulsepass.persistence;

import com.pulsepass.domain.Event;
import com.pulsepass.domain.EventStatus;
import com.pulsepass.domain.Ticket;
import com.pulsepass.domain.TicketStatus;
import com.pulsepass.domain.TicketType;
import com.pulsepass.domain.User;
import com.pulsepass.domain.Venue;
import com.pulsepass.repository.EventRepository;
import com.pulsepass.repository.TicketRepository;
import com.pulsepass.repository.UserRepository;
import com.pulsepass.repository.VenueRepository;
import com.pulsepass.support.AbstractPostgresIT;
import com.pulsepass.support.TestData;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


class TicketRepositoryIT extends AbstractPostgresIT {

    @Autowired
    private VenueRepository venueRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbc;


    @BeforeEach
    void setUp() {
        Venue venue = venueRepository.save(TestData.marinaVenue());
        Event cmf = eventRepository.save(TestData.caribbeanMusicFest(venue));
        User andrea = userRepository.save(TestData.user("andrea"));
        User carlos = userRepository.save(TestData.user("carlos"));
        User laura = userRepository.save(TestData.user("laura"));
        User miguel = userRepository.save(TestData.user("miguel"));

        ticketRepository.saveAll(List.of(
                TestData.ticket("TCK-0001", TicketType.VIP, "250000", TicketStatus.PAID, andrea, cmf),
                TestData.ticket("TCK-0002", TicketType.GENERAL, "120000", TicketStatus.PAID, carlos, cmf),
                TestData.ticket("TCK-0003", TicketType.GENERAL, "120000", TicketStatus.RESERVED, laura, cmf),
                TestData.ticket("TCK-0004", TicketType.VIP, "250000", TicketStatus.CANCELLED, miguel, cmf)));
        ticketRepository.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("QT-006 / FR-TKT-001: un ticket navega hacia su usuario y su evento")
    void ticketNavigatesToUserAndEvent() {
        Ticket ticket = ticketRepository.findByTicketCode("TCK-0001").orElseThrow();

        assertThat(ticket.getUser().getEmail()).isEqualTo("andrea@pulsepass.test");
        assertThat(ticket.getEvent().getEventCode()).isEqualTo("CMF-2026");
        assertThat(ticket.getType()).isEqualTo(TicketType.VIP);
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.PAID);
        assertThat(ticket.getPurchaseDate()).isEqualTo(TestData.PURCHASE_DATE);
    }

    @Test
    @DisplayName("BR-007 / NFR-008: el precio se conserva con precision decimal (NUMERIC)")
    void keepsDecimalPrecision() {
        Ticket saved = ticketRepository.saveAndFlush(TestData.ticket(
                "TCK-DEC", TicketType.STUDENT, "99999.99", TicketStatus.RESERVED,
                userRepository.findByUsername("andrea").orElseThrow(),
                eventRepository.findByEventCode("CMF-2026").orElseThrow()));
        entityManager.clear();

        Ticket reloaded = ticketRepository.findById(saved.getId()).orElseThrow();

        assertThat(reloaded.getPrice()).isEqualByComparingTo(new BigDecimal("99999.99"));
    }

    @Test
    @DisplayName("AC-005 / FR-TKT-002: PostgreSQL rechaza un ticketCode duplicado")
    void rejectsDuplicateTicketCode() {
        Ticket duplicate = TestData.ticket("TCK-0001", TicketType.GENERAL, "120000", TicketStatus.RESERVED,
                userRepository.findByUsername("laura").orElseThrow(),
                eventRepository.findByEventCode("CMF-2026").orElseThrow());

        assertThatThrownBy(() -> ticketRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("FR-TKT-003: el CHECK impide precios negativos")
    void rejectsNegativePrice() {
        Ticket negative = TestData.ticket("TCK-NEG", TicketType.GENERAL, "-1", TicketStatus.RESERVED,
                userRepository.findByUsername("laura").orElseThrow(),
                eventRepository.findByEventCode("CMF-2026").orElseThrow());

        assertThatThrownBy(() -> ticketRepository.saveAndFlush(negative))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("FR-TKT-003: un ticket gratuito (precio 0) es valido")
    void acceptsZeroPrice() {
        Ticket free = ticketRepository.saveAndFlush(TestData.ticket("TCK-FREE", TicketType.STUDENT, "0",
                TicketStatus.PAID,
                userRepository.findByUsername("laura").orElseThrow(),
                eventRepository.findByEventCode("CMF-2026").orElseThrow()));

        assertThat(free.getId()).isNotNull();
    }

    @Test
    @DisplayName("FR-TKT-001: no puede existir un ticket con usuario inexistente (FK)")
    void rejectsTicketWithUnknownUser() {
        Long eventId = eventRepository.findByEventCode("CMF-2026").orElseThrow().getId();

        assertThatThrownBy(() -> jdbc.update("""
                insert into tickets (ticket_code, type, price, status, purchase_date, user_id, event_id)
                values ('TCK-FK1', 'GENERAL', 100, 'RESERVED', now(), 999999, ?)
                """, eventId))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("FR-TKT-001: no puede existir un ticket con evento inexistente (FK)")
    void rejectsTicketWithUnknownEvent() {
        Long userId = userRepository.findByUsername("andrea").orElseThrow().getId();

        assertThatThrownBy(() -> jdbc.update("""
                insert into tickets (ticket_code, type, price, status, purchase_date, user_id, event_id)
                values ('TCK-FK2', 'GENERAL', 100, 'RESERVED', now(), ?, 999999)
                """, userId))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("FR-TKT-001: user_id y event_id son obligatorios (NOT NULL)")
    void rejectsTicketWithNullForeignKeys() {
        assertThatThrownBy(() -> jdbc.update("""
                insert into tickets (ticket_code, type, price, status, purchase_date, user_id, event_id)
                values ('TCK-NULL', 'GENERAL', 100, 'RESERVED', now(), null, null)
                """))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("FR-TKT-004: un tipo de ticket fuera del catalogo es rechazado por CHECK")
    void rejectsInvalidType() {
        Long userId = userRepository.findByUsername("andrea").orElseThrow().getId();
        Long eventId = eventRepository.findByEventCode("CMF-2026").orElseThrow().getId();

        assertThatThrownBy(() -> jdbc.update("""
                insert into tickets (ticket_code, type, price, status, purchase_date, user_id, event_id)
                values ('TCK-T', 'PLATINUM', 100, 'PAID', now(), ?, ?)
                """, userId, eventId)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("FR-TKT-005: un estado de ticket fuera del catalogo es rechazado por CHECK")
    void rejectsInvalidStatus() {
        Long userId = userRepository.findByUsername("andrea").orElseThrow().getId();
        Long eventId = eventRepository.findByEventCode("CMF-2026").orElseThrow().getId();

        assertThatThrownBy(() -> jdbc.update("""
                insert into tickets (ticket_code, type, price, status, purchase_date, user_id, event_id)
                values ('TCK-S', 'VIP', 100, 'REFUNDED', now(), ?, ?)
                """, userId, eventId)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("BR-008: tipo y estado se guardan por nombre estable, no por ordinal")
    void storesEnumsByName() {
        String type = jdbc.queryForObject("select type from tickets where ticket_code = 'TCK-0001'", String.class);
        String status = jdbc.queryForObject("select status from tickets where ticket_code = 'TCK-0001'", String.class);

        assertThat(type).isEqualTo("VIP");
        assertThat(status).isEqualTo("PAID");
    }

    @Test
    @DisplayName("FR-TKT-006: tickets de un usuario por email (Ticket -> User -> email)")
    void findsTicketsByUserEmail() {
        List<Ticket> tickets = ticketRepository.findByUserEmail("andrea@pulsepass.test");

        assertThat(tickets).extracting(Ticket::getTicketCode).containsExactly("TCK-0001");
    }

    @Test
    @DisplayName("FR-TKT-006: tickets de un usuario por email y estado")
    void findsTicketsByUserEmailAndStatus() {
        assertThat(ticketRepository.findByUserEmailAndStatus("andrea@pulsepass.test", TicketStatus.PAID))
                .extracting(Ticket::getTicketCode).containsExactly("TCK-0001");

        assertThat(ticketRepository.findByUserEmailAndStatus("andrea@pulsepass.test", TicketStatus.CANCELLED))
                .isEmpty();
    }

    @Test
    @DisplayName("FR-TKT-007: retorna solo los tickets PAID del evento solicitado")
    void findsPaidTicketsByEventCode() {
        Venue other = venueRepository.save(TestData.venue("VEN-BOG-01", "Bogota"));
        Event otherEvent = eventRepository.save(
                TestData.event("OTHER-1", EventStatus.PUBLISHED, LocalDateTime.of(2026, 11, 1, 20, 0), other));
        ticketRepository.saveAndFlush(TestData.ticket("TCK-OTHER", TicketType.GENERAL, "80000", TicketStatus.PAID,
                userRepository.findByUsername("andrea").orElseThrow(), otherEvent));

        List<Ticket> paid = ticketRepository.findByEventEventCodeAndStatus("CMF-2026", TicketStatus.PAID);

        assertThat(paid).extracting(Ticket::getTicketCode).containsExactlyInAnyOrder("TCK-0001", "TCK-0002");
    }

    @Test
    @DisplayName("AC-008 / FR-TKT-008: solo los tickets PAID participan del conteo de ventas")
    void countsOnlyPaidTickets() {

        assertThat(ticketRepository.countPaidByEventCode("CMF-2026")).isEqualTo(2L);
    }

    @Test
    @DisplayName("FR-TKT-008: un evento sin ventas cuenta cero")
    void countsZeroWhenNoPaidTickets() {
        assertThat(ticketRepository.countPaidByEventCode("NO-SUCH-EVENT")).isZero();
    }

    @Test
    @DisplayName("FR-SRC-004: tickets de eventos posteriores a una fecha, ordenados cronologicamente")
    void findsTicketsOfFutureEventsInChronologicalOrder() {
        Venue venue = venueRepository.findByCode("VEN-SMR-01").orElseThrow();
        Event past = eventRepository.save(
                TestData.event("PAST-1", EventStatus.FINISHED, LocalDateTime.of(2026, 6, 1, 20, 0), venue));
        Event later = eventRepository.save(
                TestData.event("LATER-1", EventStatus.PUBLISHED, LocalDateTime.of(2027, 3, 1, 20, 0), venue));
        User andreaUser = userRepository.findByUsername("andrea").orElseThrow();
        ticketRepository.save(TestData.ticket("TCK-PAST", TicketType.GENERAL, "50000", TicketStatus.USED, andreaUser, past));
        ticketRepository.saveAndFlush(TestData.ticket("TCK-LATER", TicketType.GENERAL, "90000", TicketStatus.PAID, andreaUser, later));

        List<Ticket> result = ticketRepository.findByEventDateAfter(LocalDateTime.of(2026, 9, 20, 0, 0));

        assertThat(result).extracting(Ticket::getTicketCode)
                .containsExactly("TCK-0001", "TCK-0002", "TCK-0003", "TCK-0004", "TCK-LATER");
    }
}
