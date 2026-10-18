package com.pulsepass.controller;

import com.pulsepass.domain.TicketStatus;
import com.pulsepass.domain.TicketType;
import com.pulsepass.dto.request.PurchaseTicketRequest;
import com.pulsepass.dto.response.TicketResponse;
import com.pulsepass.exception.BusinessRuleException;
import com.pulsepass.exception.ResourceNotFoundException;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TicketController.class)
class TicketControllerTest extends AbstractSecuredWebMvcTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TicketService ticketService;

    private static final String VALID_BODY = """
            {
              "userEmail": "andrea@pulsepass.com",
              "eventCode": "CMF-2026",
              "type": "GENERAL"
            }
            """;

    @Test
    @DisplayName("POST /api/tickets -> 201 con compra valida")
    void purchase_valida_retorna201() throws Exception {
        when(ticketService.purchase(any(PurchaseTicketRequest.class))).thenReturn(ticket(TicketStatus.PAID));

        mockMvc.perform(post("/api/tickets").with(asUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.ticketCode").value("TKT-0001"))
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.type").value("GENERAL"))
                .andExpect(jsonPath("$.userEmail").value("andrea@pulsepass.com"));

        ArgumentCaptor<PurchaseTicketRequest> captor = ArgumentCaptor.forClass(PurchaseTicketRequest.class);
        verify(ticketService).purchase(captor.capture());
        assertThat(captor.getValue().userEmail()).isEqualTo("andrea@pulsepass.com");
        assertThat(captor.getValue().eventCode()).isEqualTo("CMF-2026");
        assertThat(captor.getValue().type()).isEqualTo(TicketType.GENERAL);
    }

    @Test
    @DisplayName("POST /api/tickets -> 400 con request invalido y sin llamar al Service")
    void purchase_invalido_retorna400() throws Exception {
        String invalid = """
                {
                  "userEmail": "no-es-email",
                  "eventCode": ""
                }
                """;

        mockMvc.perform(post("/api/tickets").with(asUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalid))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.details.userEmail").value("User email format is invalid"))
                .andExpect(jsonPath("$.details.eventCode").value("Event code is required"))
                .andExpect(jsonPath("$.details.type").value("Ticket type is required"));

        verify(ticketService, never()).purchase(any());
    }

    @Test
    @DisplayName("POST /api/tickets -> 400 con tipo de ticket inexistente")
    void purchase_tipoInvalido_retorna400() throws Exception {
        String invalid = VALID_BODY.replace("GENERAL", "NO_EXISTE");

        mockMvc.perform(post("/api/tickets").with(asUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalid))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed or invalid JSON request"));

        verify(ticketService, never()).purchase(any());
    }

    @Test
    @DisplayName("POST /api/tickets -> 404 cuando el usuario no existe")
    void purchase_usuarioInexistente_retorna404() throws Exception {
        when(ticketService.purchase(any(PurchaseTicketRequest.class)))
                .thenThrow(new ResourceNotFoundException("User not found: andrea@pulsepass.com"));

        mockMvc.perform(post("/api/tickets").with(asUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("User not found: andrea@pulsepass.com"));
    }

    @Test
    @DisplayName("POST /api/tickets -> 409 cuando el Service rechaza por regla de negocio (menor de edad)")
    void purchase_reglaDeNegocio_retorna409() throws Exception {
        when(ticketService.purchase(any(PurchaseTicketRequest.class)))
                .thenThrow(new BusinessRuleException("User does not meet the minimum age"));

        mockMvc.perform(post("/api/tickets").with(asUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("User does not meet the minimum age"));
    }

    @Test
    @DisplayName("GET /api/tickets/{ticketCode} -> 200")
    void findByCode_existente_retorna200() throws Exception {
        when(ticketService.findByCode("TKT-0001")).thenReturn(ticket(TicketStatus.PAID));

        mockMvc.perform(get("/api/tickets/{ticketCode}", "TKT-0001").with(asUser()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.ticketCode").value("TKT-0001"))
                .andExpect(jsonPath("$.eventCode").value("CMF-2026"));

        verify(ticketService).findByCode("TKT-0001");
    }

    @Test
    @DisplayName("GET /api/tickets/{ticketCode} -> 404 cuando no existe")
    void findByCode_inexistente_retorna404() throws Exception {
        when(ticketService.findByCode("NOPE"))
                .thenThrow(new ResourceNotFoundException("Ticket not found: NOPE"));

        mockMvc.perform(get("/api/tickets/{ticketCode}", "NOPE").with(asUser()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Ticket not found: NOPE"));
    }

    @Test
    @DisplayName("GET /api/tickets/by-user?email=... -> 200")
    void findByUserEmail_retorna200() throws Exception {
        when(ticketService.findByUserEmail("andrea@pulsepass.com"))
                .thenReturn(List.of(ticket(TicketStatus.PAID)));

        mockMvc.perform(get("/api/tickets/by-user").with(asUser()).param("email", "andrea@pulsepass.com"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].userEmail").value("andrea@pulsepass.com"));

        verify(ticketService).findByUserEmail("andrea@pulsepass.com");
    }

    @Test
    @DisplayName("GET /api/tickets/by-user sin email -> 400")
    void findByUserEmail_sinParametro_retorna400() throws Exception {
        mockMvc.perform(get("/api/tickets/by-user").with(asUser()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Missing request parameter"))
                .andExpect(jsonPath("$.details.email").value("Parameter is required"));

        verify(ticketService, never()).findByUserEmail(anyString());
    }

    @Test
    @DisplayName("PATCH /api/tickets/{ticketCode}/cancel -> 200")
    void cancel_valido_retorna200() throws Exception {
        when(ticketService.cancel("TKT-0001")).thenReturn(ticket(TicketStatus.CANCELLED));

        mockMvc.perform(patch("/api/tickets/{ticketCode}/cancel", "TKT-0001").with(asUser()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        verify(ticketService).cancel("TKT-0001");
    }

    @Test
    @DisplayName("PATCH /api/tickets/{ticketCode}/cancel -> 409 si el ticket ya fue usado")
    void cancel_invalido_retorna409() throws Exception {
        when(ticketService.cancel("TKT-0001"))
                .thenThrow(new BusinessRuleException("A USED ticket cannot be cancelled"));

        mockMvc.perform(patch("/api/tickets/{ticketCode}/cancel", "TKT-0001").with(asUser()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("A USED ticket cannot be cancelled"));
    }

    @Test
    @DisplayName("PATCH /api/tickets/{ticketCode}/use -> 200")
    void markAsUsed_valido_retorna200() throws Exception {
        when(ticketService.markAsUsed("TKT-0001")).thenReturn(ticket(TicketStatus.USED));

        mockMvc.perform(patch("/api/tickets/{ticketCode}/use", "TKT-0001").with(asStaff()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("USED"));

        verify(ticketService).markAsUsed("TKT-0001");
    }

    @Test
    @DisplayName("PATCH /api/tickets/{ticketCode}/use -> 409 si el ticket esta CANCELLED")
    void markAsUsed_invalido_retorna409() throws Exception {
        when(ticketService.markAsUsed("TKT-0001"))
                .thenThrow(new BusinessRuleException("A CANCELLED ticket cannot be used"));

        mockMvc.perform(patch("/api/tickets/{ticketCode}/use", "TKT-0001").with(asStaff()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("A CANCELLED ticket cannot be used"));
    }

    private TicketResponse ticket(TicketStatus status) {
        return new TicketResponse(
                1L,
                "TKT-0001",
                TicketType.GENERAL,
                new BigDecimal("150000.00"),
                status,
                LocalDateTime.of(2026, 10, 1, 10, 0),
                "andrea@pulsepass.com",
                "CMF-2026",
                "Caribbean Music Fest 2026");
    }
}
