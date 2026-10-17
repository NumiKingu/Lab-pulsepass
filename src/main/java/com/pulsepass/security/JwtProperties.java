package com.pulsepass.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.time.Duration;

@ConfigurationProperties(prefix = "pulsepass.security.jwt")
public record JwtProperties(
        String issuer,
        String secretBase64,
        Duration accessTokenTtl
) {
    public JwtProperties {
        if (issuer == null || issuer.isBlank()) {
            issuer = "pulsepass-api";
        }
        if (accessTokenTtl == null) {
            accessTokenTtl = Duration.ofMinutes(15);
        }
    }
}
