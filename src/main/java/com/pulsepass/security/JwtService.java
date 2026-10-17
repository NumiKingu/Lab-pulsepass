package com.pulsepass.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    private static final int MIN_KEY_BYTES = 32; // 256 bits para HS256
    private static final String EXPECTED_ALG = "HS256";

    private final SecretKey key;
    private final String issuer;
    private final long ttlSeconds;

    public JwtService(JwtProperties properties) {
        String secret = properties.secretBase64();
        if (secret == null || secret.isBlank()) {
            // Falla el arranque: SEC-004 / 12.2 del PRD
            throw new IllegalStateException(
                    "Falta la variable de entorno PULSEPASS_JWT_SECRET_BASE64 (clave JWT en Base64)");
        }
        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(secret.trim());
        } catch (RuntimeException ex) {
            throw new IllegalStateException("PULSEPASS_JWT_SECRET_BASE64 no es Base64 valido");
        }
        if (keyBytes.length < MIN_KEY_BYTES) {
            throw new IllegalStateException(
                    "La clave JWT debe tener al menos 32 bytes aleatorios (tiene " + keyBytes.length + ")");
        }
        if (properties.accessTokenTtl().isNegative() || properties.accessTokenTtl().isZero()) {
            throw new IllegalStateException("pulsepass.security.jwt.access-token-ttl debe ser positivo");
        }
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.issuer = properties.issuer();
        this.ttlSeconds = properties.accessTokenTtl().toSeconds();
    }

    public String generateToken(Long userId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .issuer(issuer)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(ttlSeconds)))
                .id(UUID.randomUUID().toString())
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }


    public Long extractUserId(String token) {
        Jws<Claims> jws = Jwts.parser()
                .verifyWith(key)              // verifica la firma ANTES de confiar en los claims
                .requireIssuer(issuer)        // issuer esperado
                .build()
                .parseSignedClaims(token);    // rechaza tokens sin firma (alg=none)

        if (!EXPECTED_ALG.equals(jws.getHeader().getAlgorithm())) {
            throw new UnsupportedJwtException("Algoritmo JWT no permitido");
        }

        Claims claims = jws.getPayload();
        if (claims.getExpiration() == null) {
            throw new MalformedJwtException("El claim exp es obligatorio");
        }
        if (claims.getIssuedAt() == null || claims.getIssuedAt().toInstant().isAfter(Instant.now().plusSeconds(60))) {
            throw new MalformedJwtException("El claim iat es obligatorio y no puede estar en el futuro");
        }
        if (claims.getId() == null || claims.getId().isBlank()) {
            throw new MalformedJwtException("El claim jti es obligatorio");
        }
        String subject = claims.getSubject();
        if (subject == null || subject.isBlank()) {
            throw new MalformedJwtException("El claim sub es obligatorio");
        }
        try {
            return Long.valueOf(subject);
        } catch (NumberFormatException ex) {
            throw new MalformedJwtException("El claim sub no es un ID valido");
        }
    }

    public long getAccessTokenTtlSeconds() {
        return ttlSeconds;
    }
}
