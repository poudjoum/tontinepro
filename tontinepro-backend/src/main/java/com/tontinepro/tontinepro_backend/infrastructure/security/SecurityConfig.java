package com.tontinepro.tontinepro_backend.infrastructure.security;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final CorsConfigurationSource corsConfigurationSource;

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/v1/auth/register",
                                "/api/v1/auth/login",
                                "/api/v1/auth/refresh",
                                "/api/v1/auth/2fa/valider",
                                "/api/v1/setup",
                                "/api/v1/setup/status",
                                "/api/v1/invitation/*/statut",
                                "/api/v1/invitation/*/rejoindre",
                                "/api/v1/tontines/publiques",
                                "/api/v1/tontines/creer-avec-compte",
                                "/api/v1/tontines/*/demandes",
                                "/api/v1/tontines/*/documents-officiels",
                                "/api/v1/tontines/*/documents-officiels/*/fichier",
                                "/api/v1/auth/reset-password/verify",
                                "/api/v1/auth/reset-password/confirmer",
                                "/api/v1/auth/mot-de-passe-oublie",
                                "/api/v1/auth/activer-compte",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                // Sans point d'entrée explicite, Spring Security se rabat sur
                // Http403ForbiddenEntryPoint : une requête non authentifiée (jeton
                // expiré ou absent) reçoit alors 403 au lieu de 401. Le client ne
                // peut plus distinguer « session expirée » de « droits
                // insuffisants », et son rafraîchissement de jeton — déclenché sur
                // 401 — ne partait jamais. On rétablit la sémantique :
                //   401 = non authentifié → le client rafraîchit son jeton
                //   403 = authentifié mais pas autorisé → refus définitif
                .exceptionHandling(ex -> ex.authenticationEntryPoint(
                        (request, response, authException) -> response.sendError(
                                HttpServletResponse.SC_UNAUTHORIZED, "Authentification requise")))
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
