package com.pulsepass.controller;

import com.pulsepass.domain.Role;
import com.pulsepass.dto.request.LoginRequest;
import com.pulsepass.dto.response.AuthResponse;
import com.pulsepass.dto.response.AuthenticatedUserResponse;
import com.pulsepass.security.JwtService;
import com.pulsepass.security.SecurityPrincipal;
import com.pulsepass.service.AuthService;
import com.pulsepass.support.AbstractSecuredWebMvcTest;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Date;
import java.util.Set;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
class AuthControllerTest extends AbstractSecuredWebMvcTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private JwtService jwtService;
    @MockitoBean private AuthService authService;

    private static final String LOGIN_BODY = """
            { "identifier": "andrea", "password": "UnaFraseSeguraLarga2026!" }
            """;

    @Test
    @DisplayName("T-SEC-005: POST /api/auth/login correcto -> 200 con Bearer")
    void loginCorrecto() throws Exception {
        when(authService.login(any(LoginRequest.class))).thenReturn(new AuthResponse("jwt", "Bearer", 900));

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(LOGIN_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("jwt"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("no-store")));
    }

    @Test
    @DisplayName("T-SEC-006/019: login con contraseña incorrecta -> 401 generico, sin stacktrace")
    void loginIncorrecto() throws Exception {
        when(authService.login(any(LoginRequest.class))).thenThrow(new BadCredentialsException("Invalid credentials"));

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(LOGIN_BODY))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value(not(containsString("Exception"))))
                .andExpect(jsonPath("$.details").isMap());
    }

    @Test
    @DisplayName("Login con body invalido -> 400 de Bean Validation")
    void loginInvalido() throws Exception {
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.identifier").value("Identifier is required"));
    }

    @Test
    @DisplayName("GET /api/auth/me con JWT valido -> 200")
    void meConTokenValido() throws Exception {
        String token = jwtService.generateToken(1L);
        when(customUserDetailsService.loadUserById(1L)).thenReturn(principal(1L, "andrea", Role.USER));
        when(authService.me(any(SecurityPrincipal.class)))
                .thenReturn(new AuthenticatedUserResponse(1L, "andrea", Set.of("USER")));

        mockMvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("andrea"));
    }

    @Test
    @DisplayName("GET /api/auth/me sin token -> 401 con ErrorResponse")
    void meSinToken() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    @DisplayName("T-SEC-007: token expirado -> 401 con ErrorResponse")
    void tokenExpirado() throws Exception {
        Instant now = Instant.now();
        String expired = Jwts.builder()
                .subject("1").issuer("pulsepass-api").id("jti")
                .issuedAt(Date.from(now.minusSeconds(3600)))
                .expiration(Date.from(now.minusSeconds(10)))
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(TEST_SECRET)), Jwts.SIG.HS256)
                .compact();

        mockMvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + expired))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
        verify(customUserDetailsService, never()).loadUserById(anyLong());
    }

    @Test
    @DisplayName("T-SEC-008: firma alterada -> 401 y no se consulta al usuario")
    void firmaAlterada() throws Exception {
        String[] parts = jwtService.generateToken(1L).split("\\.");
        String otroPayload = jwtService.generateToken(999L).split("\\.")[1];
        String alterado = parts[0] + "." + otroPayload + "." + parts[2];

        mockMvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + alterado))
                .andExpect(status().isUnauthorized());
        verify(customUserDetailsService, never()).loadUserById(anyLong());
        verify(authService, never()).me(any());
    }

    @Test
    @DisplayName("T-SEC-016: token valido de usuario INACTIVO -> 401")
    void usuarioInactivo() throws Exception {
        String token = jwtService.generateToken(1L);
        when(customUserDetailsService.loadUserById(1L))
                .thenReturn(new SecurityPrincipal(1L, "andrea", null, false, Set.of(Role.USER)));

        mockMvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized());
        verify(authService, never()).me(any());
    }

    @Test
    @DisplayName("Bearer malformado / esquema distinto / doble header -> 401, nunca 500")
    void entradasMalformadas() throws Exception {
        mockMvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer basura"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer "))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Basic YW5kcmVhOjEyMw=="))
                .andExpect(status().isUnauthorized());

        String token = jwtService.generateToken(1L);
        mockMvc.perform(get("/api/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("FR-SEC-AUTH-007: el JWT en query string se ignora -> 401")
    void tokenEnQueryStringNoSirve() throws Exception {
        String token = jwtService.generateToken(1L);

        mockMvc.perform(get("/api/auth/me").param("access_token", token))
                .andExpect(status().isUnauthorized());
    }
}
