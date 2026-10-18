package com.pulsepass.integration;

import com.jayway.jsonpath.JsonPath;
import com.pulsepass.domain.Event;
import com.pulsepass.domain.EventCategory;
import com.pulsepass.domain.EventStatus;
import com.pulsepass.domain.Role;
import com.pulsepass.domain.User;
import com.pulsepass.domain.UserCredential;
import com.pulsepass.domain.UserRole;
import com.pulsepass.domain.Venue;
import com.pulsepass.repository.EventRepository;
import com.pulsepass.repository.TicketRepository;
import com.pulsepass.repository.UserCredentialRepository;
import com.pulsepass.repository.UserRepository;
import com.pulsepass.repository.UserRoleRepository;
import com.pulsepass.repository.VenueRepository;
import com.pulsepass.support.AbstractSecuredWebMvcTest;
import com.pulsepass.support.PostgresTestContainerConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(PostgresTestContainerConfig.class)
@TestPropertySource(properties = "pulsepass.security.jwt.secret-base64=" + AbstractSecuredWebMvcTest.TEST_SECRET)
@Transactional
class SecurityFlowIT {
    private static final String PASSWORD = "UnaFraseSeguraLarga2026!";

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private UserCredentialRepository credentialRepository;
    @Autowired private UserRoleRepository roleRepository;
    @Autowired private VenueRepository venueRepository;
    @Autowired private EventRepository eventRepository;
    @Autowired private TicketRepository ticketRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @BeforeEach
    void escenario() {
        Venue venue = venueRepository.save(new Venue("VEN-SMR-01", "Marina Convention Center",
                "Santa Marta", "Calle 1 # 1-01", 5000));
        LocalDateTime fecha = LocalDateTime.now().plusMonths(2);
        eventRepository.save(new Event("CMF-2026", "Caribbean Music Fest", EventCategory.MUSIC,
                EventStatus.PUBLISHED, fecha, venue));
        eventRepository.save(new Event("DRAFT-1", "Evento borrador", EventCategory.MUSIC,
                EventStatus.DRAFT, fecha, venue));
    }

    @Test
    @DisplayName("UC-SEC-01/02: registro atomico con hash BCrypt + ROLE_USER, y login con JWT de 900 s")
    void registroYLogin() throws Exception {
        registrar("andrea");

        User andrea = userRepository.findByUsername("andrea").orElseThrow();
        String hash = credentialRepository.findPasswordHashByUserId(andrea.getId()).orElseThrow();
        assertThat(hash).startsWith("$2").isNotEqualTo(PASSWORD);
        assertThat(roleRepository.findRolesByUserId(andrea.getId())).containsExactly(Role.USER);

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("andrea", PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900));

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("andrea", "ContraseñaIncorrecta1")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("UC-SEC-04/05/06: compra propia OK; compra para otro y lectura/cancelacion ajena -> 403")
    void propiedadDeTickets() throws Exception {
        registrar("andrea");
        registrar("carlos");
        String tokenAndrea = login("andrea");
        String tokenCarlos = login("carlos");

        String ticketAndrea = comprar(tokenAndrea, "andrea@pulsepass.com", 201);

        long antes = ticketRepository.count();
        comprar(tokenAndrea, "carlos@pulsepass.com", 403);
        assertThat(ticketRepository.count()).isEqualTo(antes);

        String ticketCarlos = comprar(tokenCarlos, "carlos@pulsepass.com", 201);

        mockMvc.perform(get("/api/tickets/{c}", ticketCarlos).header(HttpHeaders.AUTHORIZATION, bearer(tokenAndrea)))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/tickets/{c}/cancel", ticketCarlos).header(HttpHeaders.AUTHORIZATION, bearer(tokenAndrea)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/tickets/{c}", ticketAndrea).header(HttpHeaders.AUTHORIZATION, bearer(tokenAndrea)))
                .andExpect(status().isOk());
        mockMvc.perform(patch("/api/tickets/{c}/cancel", ticketAndrea).header(HttpHeaders.AUTHORIZATION, bearer(tokenAndrea)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        mockMvc.perform(get("/api/users/by-email").param("email", "carlos@pulsepass.com")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenAndrea)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/tickets/by-user").param("email", "carlos@pulsepass.com")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenAndrea)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/users/by-username").param("username", "andrea")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenAndrea)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("UC-SEC-07: STAFF marca uso; el segundo uso lo rechaza la regla heredada (409)")
    void staffMarcaUso() throws Exception {
        registrar("andrea");
        String ticket = comprar(login("andrea"), "andrea@pulsepass.com", 201);
        crearCuentaPrivilegiada("laura", Role.STAFF);
        String tokenLaura = login("laura");

        mockMvc.perform(patch("/api/tickets/{c}/use", ticket).header(HttpHeaders.AUTHORIZATION, bearer(tokenLaura)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("USED"));
        mockMvc.perform(patch("/api/tickets/{c}/use", ticket).header(HttpHeaders.AUTHORIZATION, bearer(tokenLaura)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("T-SEC-017: borrador oculto para anonimo/USER, visible para STAFF")
    void borradores() throws Exception {
        mockMvc.perform(get("/api/events/{c}", "CMF-2026")).andExpect(status().isOk());
        mockMvc.perform(get("/api/events/{c}", "DRAFT-1"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.name").doesNotExist());

        crearCuentaPrivilegiada("laura", Role.STAFF);
        mockMvc.perform(get("/api/events/{c}", "DRAFT-1").header(HttpHeaders.AUTHORIZATION, bearer(login("laura"))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("UC-SEC-10: usuario desactivado con token vigente -> 401 en el siguiente request")
    void usuarioDesactivado() throws Exception {
        registrar("andrea");
        String token = login("andrea");
        mockMvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, bearer(token))).andExpect(status().isOk());

        User andrea = userRepository.findByUsername("andrea").orElseThrow();
        andrea.setActive(false);
        userRepository.saveAndFlush(andrea);

        mockMvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isUnauthorized());
    }

    private void registrar(String username) throws Exception {
        String body = """
                {
                  "username": "%s", "email": "%s@pulsepass.com",
                  "firstName": "Nombre", "lastName": "Apellido",
                  "city": "Santa Marta", "birthDate": "1998-05-10",
                  "password": "%s"
                }
                """.formatted(username, username, PASSWORD);
        mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    private String login(String identifier) throws Exception {
        String json = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(identifier, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.accessToken");
    }

    private String comprar(String token, String email, int expectedStatus) throws Exception {
        String body = """
                { "userEmail": "%s", "eventCode": "CMF-2026", "type": "GENERAL" }
                """.formatted(email);
        String json = mockMvc.perform(post("/api/tickets").header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().is(expectedStatus))
                .andReturn().getResponse().getContentAsString();
        return expectedStatus == 201 ? JsonPath.read(json, "$.ticketCode") : null;
    }

    private void crearCuentaPrivilegiada(String username, Role role) {
        User user = userRepository.save(new User(username, username + "@pulsepass.com"));
        credentialRepository.save(new UserCredential(user, passwordEncoder.encode(PASSWORD)));
        roleRepository.save(new UserRole(user, role));
    }

    private static String loginBody(String identifier, String password) {
        return """
                { "identifier": "%s", "password": "%s" }
                """.formatted(identifier, password);
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}
