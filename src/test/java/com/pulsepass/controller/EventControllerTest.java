package com.pulsepass.controller;

import com.pulsepass.domain.EventCategory;
import com.pulsepass.domain.EventStatus;
import com.pulsepass.domain.TicketStatus;
import com.pulsepass.domain.TicketType;
import com.pulsepass.dto.request.CreateEventRequest;
import com.pulsepass.dto.response.ArtistResponse;
import com.pulsepass.dto.response.EventResponse;
import com.pulsepass.dto.response.EventSummaryResponse;
import com.pulsepass.dto.response.TicketResponse;
import com.pulsepass.exception.BusinessRuleException;
import com.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.service.EventService;
import com.pulsepass.service.TicketService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import com.pulsepass.support.AbstractSecuredWebMvcTest;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EventController.class)
class EventControllerTest extends AbstractSecuredWebMvcTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EventService eventService;

    @MockitoBean
    private TicketService ticketService;

    private static final String VALID_BODY = """
            {
              "eventCode": "CMF-2026",
              "name": "Caribbean Music Fest 2026",
              "description": "Festival de musica en Santa Marta",
              "category": "MUSIC",
              "eventDate": "2026-12-20T20:00:00",
              "minimumAge": 18,
              "venueCode": "VEN-SMR-01"
            }
            """;

    @Test
    @DisplayName("POST /api/events -> 201 con request valido")
    void create_valido_retorna201() throws Exception {
        when(eventService.create(any(CreateEventRequest.class))).thenReturn(event(EventStatus.DRAFT));

        mockMvc.perform(post("/api/events").with(asAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.eventCode").value("CMF-2026"))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.category").value("MUSIC"))
                .andExpect(jsonPath("$.venueCode").value("VEN-SMR-01"));

        ArgumentCaptor<CreateEventRequest> captor = ArgumentCaptor.forClass(CreateEventRequest.class);
        verify(eventService).create(captor.capture());
        assertThat(captor.getValue().eventCode()).isEqualTo("CMF-2026");
        assertThat(captor.getValue().minimumAge()).isEqualTo(18);
        assertThat(captor.getValue().category()).isEqualTo(EventCategory.MUSIC);
    }

    @Test
    @DisplayName("POST /api/events -> 400 con Bean Validation y sin llamar al Service")
    void create_invalido_retorna400() throws Exception {
        String invalid = """
                {
                  "eventCode": "",
                  "name": "Caribbean Music Fest 2026",
                  "category": "MUSIC",
                  "eventDate": "2026-12-20T20:00:00",
                  "minimumAge": 18,
                  "venueCode": ""
                }
                """;

        mockMvc.perform(post("/api/events").with(asAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalid))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.details.eventCode").value("Event code is required"))
                .andExpect(jsonPath("$.details.venueCode").value("Venue code is required"));

        verify(eventService, never()).create(any());
    }

    @Test
    @DisplayName("POST /api/events -> 400 con minimumAge negativo")
    void create_edadMinimaNegativa_retorna400() throws Exception {
        String invalid = VALID_BODY.replace("\"minimumAge\": 18", "\"minimumAge\": -1");

        mockMvc.perform(post("/api/events").with(asAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalid))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.minimumAge").value("Minimum age cannot be negative"));

        verify(eventService, never()).create(any());
    }

    @Test
    @DisplayName("POST /api/events -> 400 con JSON mal formado")
    void create_jsonMalFormado_retorna400() throws Exception {
        mockMvc.perform(post("/api/events").with(asAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ esto no es json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Malformed or invalid JSON request"));

        verify(eventService, never()).create(any());
    }

    @Test
    @DisplayName("POST /api/events -> 400 con categoria inexistente")
    void create_categoriaInvalida_retorna400() throws Exception {
        String invalid = VALID_BODY.replace("\"MUSIC\"", "\"NO_EXISTE\"");

        mockMvc.perform(post("/api/events").with(asAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalid))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed or invalid JSON request"));

        verify(eventService, never()).create(any());
    }

    @Test
    @DisplayName("POST /api/events -> 404 cuando el venue no existe")
    void create_venueInexistente_retorna404() throws Exception {
        when(eventService.create(any(CreateEventRequest.class)))
                .thenThrow(new ResourceNotFoundException("Venue not found: VEN-SMR-01"));

        mockMvc.perform(post("/api/events").with(asAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Venue not found: VEN-SMR-01"));
    }

    @Test
    @DisplayName("GET /api/events/{eventCode} -> 200 cuando existe")
    void findByCode_existente_retorna200() throws Exception {
        when(eventService.findByCode("CMF-2026")).thenReturn(event(EventStatus.DRAFT));

        mockMvc.perform(get("/api/events/{eventCode}", "CMF-2026"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.eventCode").value("CMF-2026"))
                .andExpect(jsonPath("$.name").value("Caribbean Music Fest 2026"))
                .andExpect(jsonPath("$.venueName").value("Marina Convention Center"));

        verify(eventService).findByCode("CMF-2026");
    }

    @Test
    @DisplayName("GET /api/events/{eventCode} -> 404 cuando no existe")
    void findByCode_inexistente_retorna404() throws Exception {
        when(eventService.findByCode("X-1"))
                .thenThrow(new ResourceNotFoundException("Event not found: X-1"));

        mockMvc.perform(get("/api/events/{eventCode}", "X-1"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Event not found: X-1"));
    }

    @Test
    @DisplayName("GET /api/events/published -> 200")
    void findPublishedEvents_retorna200() throws Exception {
        when(eventService.findPublishedEvents()).thenReturn(List.of(summary()));

        mockMvc.perform(get("/api/events/published"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].eventCode").value("CMF-2026"))
                .andExpect(jsonPath("$[0].status").value("PUBLISHED"));

        verify(eventService).findPublishedEvents();
        verify(eventService, never()).findByCode(any());
    }

    @Test
    @DisplayName("PATCH /api/events/{eventCode}/publish -> 200")
    void publish_valido_retorna200() throws Exception {
        when(eventService.publish("CMF-2026")).thenReturn(event(EventStatus.PUBLISHED));

        mockMvc.perform(patch("/api/events/{eventCode}/publish", "CMF-2026").with(asAdmin()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("PUBLISHED"));

        verify(eventService).publish("CMF-2026");
    }

    @Test
    @DisplayName("PATCH /api/events/{eventCode}/publish -> 409 si la regla de negocio falla")
    void publish_invalido_retorna409() throws Exception {
        when(eventService.publish("CMF-2026"))
                .thenThrow(new BusinessRuleException("Only DRAFT events can be published"));

        mockMvc.perform(patch("/api/events/{eventCode}/publish", "CMF-2026").with(asAdmin()))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Only DRAFT events can be published"));
    }

    @Test
    @DisplayName("POST /api/events/{eventCode}/artists/{artistId} -> 200")
    void addArtist_valido_retorna200() throws Exception {
        when(eventService.addArtist("CMF-2026", 1L)).thenReturn(event(EventStatus.DRAFT));

        mockMvc.perform(post("/api/events/{eventCode}/artists/{artistId}", "CMF-2026", 1L).with(asAdmin()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.eventCode").value("CMF-2026"))
                .andExpect(jsonPath("$.artists.length()").value(1))
                .andExpect(jsonPath("$.artists[0].stageName").value("Solar Beat"));

        verify(eventService).addArtist("CMF-2026", 1L);
    }

    @Test
    @DisplayName("POST /api/events/{eventCode}/artists/{artistId} -> 409 si ya esta asociado")
    void addArtist_duplicado_retorna409() throws Exception {
        when(eventService.addArtist("CMF-2026", 1L))
                .thenThrow(new BusinessRuleException("Artist already associated to event"));

        mockMvc.perform(post("/api/events/{eventCode}/artists/{artistId}", "CMF-2026", 1L).with(asAdmin()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Artist already associated to event"));
    }

    @Test
    @DisplayName("POST /api/events/{eventCode}/artists/{artistId} -> 404 si el artista no existe")
    void addArtist_artistaInexistente_retorna404() throws Exception {
        when(eventService.addArtist("CMF-2026", 99L))
                .thenThrow(new ResourceNotFoundException("Artist not found: 99"));

        mockMvc.perform(post("/api/events/{eventCode}/artists/{artistId}", "CMF-2026", 99L).with(asAdmin()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Artist not found: 99"));
    }

    @Test
    @DisplayName("GET /api/events/by-artist?stageName=... -> 200")
    void findByArtist_retorna200() throws Exception {
        when(eventService.findByArtist("Solar Beat")).thenReturn(List.of(summary()));

        mockMvc.perform(get("/api/events/by-artist").param("stageName", "Solar Beat"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].eventCode").value("CMF-2026"));

        verify(eventService).findByArtist("Solar Beat");
    }

    @Test
    @DisplayName("GET /api/events/{eventCode}/tickets/paid -> 200 con tickets PAID")
    void findPaidTickets_retorna200() throws Exception {
        when(ticketService.findPaidTicketsByEvent("CMF-2026")).thenReturn(List.of(paidTicket()));

        mockMvc.perform(get("/api/events/{eventCode}/tickets/paid", "CMF-2026").with(asStaff()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].ticketCode").value("TKT-0001"))
                .andExpect(jsonPath("$[0].status").value("PAID"));

        verify(ticketService).findPaidTicketsByEvent("CMF-2026");
    }

    @Test
    @DisplayName("GET /api/events/{eventCode}/tickets/paid -> 404 si el evento no existe")
    void findPaidTickets_eventoInexistente_retorna404() throws Exception {
        when(ticketService.findPaidTicketsByEvent("X-1"))
                .thenThrow(new ResourceNotFoundException("Event not found: X-1"));

        mockMvc.perform(get("/api/events/{eventCode}/tickets/paid", "X-1").with(asStaff()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Event not found: X-1"));
    }

    private EventResponse event(EventStatus status) {
        return new EventResponse(
                1L,
                "CMF-2026",
                "Caribbean Music Fest 2026",
                "Festival de musica en Santa Marta",
                EventCategory.MUSIC,
                status,
                LocalDateTime.of(2026, 12, 20, 20, 0),
                18,
                "VEN-SMR-01",
                "Marina Convention Center",
                List.of(new ArtistResponse(1L, "Solar Beat", "Colombia", "Electronic", true)));
    }

    private EventSummaryResponse summary() {
        return new EventSummaryResponse(
                1L,
                "CMF-2026",
                "Caribbean Music Fest 2026",
                EventCategory.MUSIC,
                EventStatus.PUBLISHED,
                LocalDateTime.of(2026, 12, 20, 20, 0),
                "Marina Convention Center",
                "Santa Marta");
    }

    private TicketResponse paidTicket() {
        return new TicketResponse(
                1L,
                "TKT-0001",
                TicketType.GENERAL,
                new BigDecimal("150000.00"),
                TicketStatus.PAID,
                LocalDateTime.of(2026, 10, 1, 10, 0),
                "andrea@pulsepass.com",
                "CMF-2026",
                "Caribbean Music Fest 2026");
    }
}
