package com.pulsepass.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
                                            JwtService jwtService,
                                            CustomUserDetailsService userDetailsService,
                                            RestAuthenticationEntryPoint authEntryPoint,
                                            RestAccessDeniedHandler deniedHandler) throws Exception {

        JwtAuthenticationFilter jwtFilter =
                new JwtAuthenticationFilter(jwtService, userDetailsService, authEntryPoint);

        http
                // Solo API Bearer, sin cookies de autenticacion -> CSRF no aplica (revisar si eso cambia)
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authEntryPoint)   // 401
                        .accessDeniedHandler(deniedHandler))        // 403
                .authorizeHttpRequests(auth -> auth
                        // ---- Autenticacion y registro (publicos) ----
                        .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/users").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/auth/me").authenticated()

                        // ---- Venues y artistas: lectura publica (VEN-01/02, ART-01..03) ----
                        .requestMatchers(HttpMethod.GET, "/api/venues/**", "/api/artists/**").permitAll()

                        // ---- Eventos: rutas especificas ANTES que /api/events/* ----
                        .requestMatchers(HttpMethod.GET, "/api/events/published").permitAll()          // EVT-03
                        .requestMatchers(HttpMethod.GET, "/api/events/by-artist").permitAll()          // EVT-06 (filtrado en Service)
                        .requestMatchers(HttpMethod.GET, "/api/events/*/tickets/paid")
                        .hasAnyRole("STAFF", "ADMIN")                                              // TKT-04
                        .requestMatchers(HttpMethod.GET, "/api/events/*").permitAll()                  // EVT-02 (borradores protegidos en Service)
                        .requestMatchers(HttpMethod.POST, "/api/events").hasRole("ADMIN")              // EVT-01
                        .requestMatchers(HttpMethod.PATCH, "/api/events/*/publish").hasRole("ADMIN")   // EVT-04
                        .requestMatchers(HttpMethod.POST, "/api/events/*/artists/*").hasRole("ADMIN")  // EVT-05

                        // ---- Tickets ----
                        .requestMatchers(HttpMethod.POST, "/api/tickets").hasRole("USER")               // TKT-01
                        .requestMatchers(HttpMethod.PATCH, "/api/tickets/*/use")
                        .hasAnyRole("STAFF", "ADMIN")                                              // TKT-06
                        .requestMatchers(HttpMethod.PATCH, "/api/tickets/*/cancel")
                        .hasAnyRole("USER", "ADMIN")                                               // TKT-05 (+ propiedad)
                        .requestMatchers(HttpMethod.GET, "/api/tickets/**").authenticated()            // TKT-02/03 (+ propiedad)

                        // ---- Usuarios: busquedas solo autenticado (+ propiedad en Service) ----
                        .requestMatchers(HttpMethod.GET, "/api/users/**").authenticated()              // USR-02/03

                        .requestMatchers("/error").permitAll()
                        // SEC-001: todo lo que no este en la matriz queda DENEGADO
                        .anyRequest().denyAll())
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    AuthenticationManager authenticationManager(CustomUserDetailsService userDetailsService,
                                                PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(
            @Value("${pulsepass.security.cors.allowed-origins:}") List<String> allowedOrigins) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins.stream().filter(o -> !o.isBlank()).toList());
        config.setAllowedMethods(List.of("GET", "POST", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setAllowCredentials(false);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
