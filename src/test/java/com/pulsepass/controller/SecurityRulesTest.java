package com.pulsepass.controller;

import com.pulsepass.dto.request.CreateEventRequest;
import com.pulsepass.service.ArtistService;
import com.pulsepass.service.AuthService;
import com.pulsepass.service.EventService;
import com.pulsepass.service.TicketService;
import com.pulsepass.service.UserService;
import com.pulsepass.service.VenueService;
import com.pulsepass.support.AbstractSecuredWebMvcTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({EventController.class, TicketController.class, UserController.class,
        VenueController.class, ArtistController.class})
class SecurityRulesTest extends AbstractSecuredWebMvcTest {
    @Autowired private MockMvc mockMvc;

    @MockitoBean private EventService eventService;
    @MockitoBean private TicketService ticketService;
    @MockitoBean private UserService userService;
    @MockitoBean private AuthService authService;
    @MockitoBean private VenueService venueService;
    @MockitoBean private ArtistService artistService;

    private static final String EVENT_BODY = """
            {
              "eventCode": "CMF-2026", "name": "Caribbean Music Fest 2026",
              "category": "MUSIC", "eventDate": "2030-12-20T20:00:00",
              "minimumAge": 18, "venueCode": "VEN-SMR-01"
            }
            """;

    @Test
    @DisplayName("T-SEC-001 + VEN/ART: lecturas publicas -> 200 sin token y sin cookie de sesion")
    void lecturasPublicas() throws Exception {
        mockMvc.perform(get("/api/events/published"))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("Set-Cookie"));
        mockMvc.perform(get("/api/venues/active")).andExpect(status().isOk());
        mockMvc.perform(get("/api/venues/{code}", "VEN-SMR-01")).andExpect(status().isOk());
        mockMvc.perform(get("/api/artists/active")).andExpect(status().isOk());
        mockMvc.perform(get("/api/events/by-artist").param("stageName", "Solar Beat")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("T-SEC-002: anonimo POST /api/events -> 401 y no se invoca el Service")
    void anonimoCreaEvento() throws Exception {
        mockMvc.perform(post("/api/events").contentType(MediaType.APPLICATION_JSON).content(EVENT_BODY))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.timestamp").exists());
        verify(eventService, never()).create(any());
    }

    @Test
    @DisplayName("T-SEC-003: USER POST /api/events -> 403 sin invocar Service")
    void userCreaEvento() throws Exception {
        mockMvc.perform(post("/api/events").with(asUser()).contentType(MediaType.APPLICATION_JSON).content(EVENT_BODY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("No tiene permisos para realizar esta operacion"));
        verify(eventService, never()).create(any());
    }

    @Test
    @DisplayName("T-SEC-004: ADMIN POST /api/events valido -> 201")
    void adminCreaEvento() throws Exception {
        mockMvc.perform(post("/api/events").with(asAdmin()).contentType(MediaType.APPLICATION_JSON).content(EVENT_BODY))
                .andExpect(status().isCreated());
        verify(eventService).create(any(CreateEventRequest.class));
    }

    @Test
    @DisplayName("T-SEC-018: ADMIN con body invalido -> 400 de Bean Validation")
    void adminBodyInvalido() throws Exception {
        mockMvc.perform(post("/api/events").with(asAdmin()).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"));
    }

    @Test
    @DisplayName("T-SEC-014 / UC-SEC-08: publicar -> ADMIN 200, USER 403, STAFF 403")
    void publicarEvento() throws Exception {
        mockMvc.perform(patch("/api/events/{c}/publish", "CMF-2026").with(asAdmin())).andExpect(status().isOk());
        mockMvc.perform(patch("/api/events/{c}/publish", "CMF-2026").with(asUser())).andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/events/{c}/publish", "CMF-2026").with(asStaff())).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("EVT-05: asociar artista -> solo ADMIN")
    void asociarArtista() throws Exception {
        mockMvc.perform(post("/api/events/{c}/artists/{id}", "CMF-2026", 1L).with(asAdmin())).andExpect(status().isOk());
        mockMvc.perform(post("/api/events/{c}/artists/{id}", "CMF-2026", 1L).with(asUser())).andExpect(status().isForbidden());
        mockMvc.perform(post("/api/events/{c}/artists/{id}", "CMF-2026", 1L)).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("TKT-04: tickets pagados de un evento -> STAFF/ADMIN 200, USER 403, anonimo 401")
    void ticketsPagados() throws Exception {
        mockMvc.perform(get("/api/events/{c}/tickets/paid", "CMF-2026").with(asStaff())).andExpect(status().isOk());
        mockMvc.perform(get("/api/events/{c}/tickets/paid", "CMF-2026").with(asAdmin())).andExpect(status().isOk());
        mockMvc.perform(get("/api/events/{c}/tickets/paid", "CMF-2026").with(asUser())).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/events/{c}/tickets/paid", "CMF-2026")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("TKT-01: comprar -> anonimo 401, STAFF 403")
    void comprarTicket() throws Exception {
        String body = """
                { "userEmail": "andrea@pulsepass.com", "eventCode": "CMF-2026", "type": "GENERAL" }
                """;
        mockMvc.perform(post("/api/tickets").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/tickets").with(asStaff()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
        verify(ticketService, never()).purchase(any());
    }

    @Test
    @DisplayName("T-SEC-012/013: marcar uso -> USER 403, STAFF 200")
    void marcarUso() throws Exception {
        mockMvc.perform(patch("/api/tickets/{c}/use", "TCK-1").with(asUser())).andExpect(status().isForbidden());
        verify(ticketService, never()).markAsUsed(anyString());

        mockMvc.perform(patch("/api/tickets/{c}/use", "TCK-1").with(asStaff())).andExpect(status().isOk());
        verify(ticketService).markAsUsed("TCK-1");
    }

    @Test
    @DisplayName("Tickets y usuarios: anonimo -> 401")
    void anonimoEnRutasPrivadas() throws Exception {
        mockMvc.perform(get("/api/tickets/{c}", "TCK-1")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/tickets/by-user").param("email", "a@a.com")).andExpect(status().isUnauthorized());
        mockMvc.perform(patch("/api/tickets/{c}/cancel", "TCK-1")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/users/by-email").param("email", "a@a.com")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/users/by-username").param("username", "andrea")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("STAFF no puede cancelar tickets (solo propietario USER o ADMIN)")
    void staffNoCancela() throws Exception {
        mockMvc.perform(patch("/api/tickets/{c}/cancel", "TCK-1").with(asStaff())).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("SEC-001: ruta fuera de la matriz -> denegada (401 anonimo / 403 autenticado)")
    void denegarPorDefecto() throws Exception {
        mockMvc.perform(delete("/api/events/{c}", "CMF-2026")).andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/events/{c}", "CMF-2026").with(asAdmin())).andExpect(status().isForbidden());
    }
}
