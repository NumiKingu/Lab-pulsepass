package com.pulsepass.security;

import com.pulsepass.support.AbstractSecuredWebMvcTest;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {
    private static final String SECRET = AbstractSecuredWebMvcTest.TEST_SECRET;
    private static final SecretKey KEY = Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET));

    private final JwtService jwtService = service("pulsepass-api", SECRET);

    @Test
    @DisplayName("Genera JWT con sub, iss, iat, exp (15 min) y jti, firmado HS256")
    void generaTokenConClaimsCorrectos() {
        String token = jwtService.generateToken(7L);

        var jws = Jwts.parser().verifyWith(KEY).build().parseSignedClaims(token);
        Claims claims = jws.getPayload();

        assertThat(jws.getHeader().getAlgorithm()).isEqualTo("HS256");
        assertThat(claims.getSubject()).isEqualTo("7");
        assertThat(claims.getIssuer()).isEqualTo("pulsepass-api");
        assertThat(claims.getId()).isNotBlank();
        long seconds = (claims.getExpiration().getTime() - claims.getIssuedAt().getTime()) / 1000;
        assertThat(seconds).isEqualTo(900);
        assertThat(jwtService.getAccessTokenTtlSeconds()).isEqualTo(900);
    }

    @Test
    @DisplayName("extractUserId devuelve el ID del claim sub")
    void extraeUserId() {
        assertThat(jwtService.extractUserId(jwtService.generateToken(42L))).isEqualTo(42L);
    }

    @Test
    @DisplayName("Rechaza token expirado")
    void rechazaTokenExpirado() {
        Instant now = Instant.now();
        String expired = Jwts.builder()
                .subject("1").issuer("pulsepass-api").id("jti-1")
                .issuedAt(Date.from(now.minusSeconds(3600)))
                .expiration(Date.from(now.minusSeconds(60)))
                .signWith(KEY, Jwts.SIG.HS256)
                .compact();

        assertThatThrownBy(() -> jwtService.extractUserId(expired)).isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    @DisplayName("Rechaza token de otro issuer")
    void rechazaIssuerIncorrecto() {
        String otroIssuer = service("otro-sistema", SECRET).generateToken(1L);

        assertThatThrownBy(() -> jwtService.extractUserId(otroIssuer)).isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("Rechaza token firmado con otra clave")
    void rechazaFirmaDeOtraClave() {
        String otraClave = Base64.getEncoder().encodeToString("otra-clave-secreta-de-32-bytes-o-mas!!".getBytes());
        String token = service("pulsepass-api", otraClave).generateToken(1L);

        assertThatThrownBy(() -> jwtService.extractUserId(token)).isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("Rechaza token con payload manipulado (sub cambiado)")
    void rechazaPayloadManipulado() {
        String[] original = jwtService.generateToken(1L).split("\\.");
        String[] otro = jwtService.generateToken(999L).split("\\.");
        String manipulado = original[0] + "." + otro[1] + "." + original[2];

        assertThatThrownBy(() -> jwtService.extractUserId(manipulado)).isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("Rechaza token sin firma (alg=none)")
    void rechazaTokenSinFirma() {
        String unsigned = Jwts.builder().subject("1").issuer("pulsepass-api").compact();

        assertThatThrownBy(() -> jwtService.extractUserId(unsigned)).isInstanceOf(UnsupportedJwtException.class);
    }

    @Test
    @DisplayName("Rechaza token vacio o malformado")
    void rechazaTokenVacioOMalformado() {
        assertThatThrownBy(() -> jwtService.extractUserId("")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> jwtService.extractUserId("abc.def")).isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("No arranca con clave ausente o de menos de 32 bytes")
    void fallaConClaveDebilOAusente() {
        String claveCorta = Base64.getEncoder().encodeToString(new byte[16]);

        assertThatThrownBy(() -> service("pulsepass-api", claveCorta)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> service("pulsepass-api", "")).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> service("pulsepass-api", null)).isInstanceOf(IllegalStateException.class);
    }

    private static JwtService service(String issuer, String secret) {
        return new JwtService(new JwtProperties(issuer, secret, Duration.ofMinutes(15)));
    }
}
