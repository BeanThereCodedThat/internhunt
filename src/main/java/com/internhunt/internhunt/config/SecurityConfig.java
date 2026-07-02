package com.internhunt.internhunt.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration.
 *
 * CSRF is disabled because:
 *   1. This is a single-user local application with no login/session.
 *   2. The frontend is a separate Vite dev server (different origin) using
 *      fetch() calls — it has no mechanism to read or forward a CSRF token.
 *   3. CSRF attacks require a browser session cookie carrying auth state.
 *      Since there is no auth here, there is nothing for CSRF to protect.
 *
 * If/when multi-user auth is added (Phase 4), revisit this — either enable
 * CSRF with a SameSite cookie strategy, or switch to stateless JWT (which
 * is inherently CSRF-safe since tokens are sent in headers, not cookies).
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig
{
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception
    {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth
                .anyRequest().permitAll()
            );

        return http.build();
    }
}
