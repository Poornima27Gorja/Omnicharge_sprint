package com.omnicharge.operator_service.config;

import com.omnicharge.operator_service.filter.JwtAuthFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)    // ← THIS was missing — @PreAuthorize won't fire without it
public class SecurityConfig {

    @Autowired
    private JwtAuthFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) ->
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized"))
                .accessDeniedHandler((request, response, accessDeniedException) ->
                    response.sendError(HttpServletResponse.SC_FORBIDDEN,
                        "Forbidden: ROLE_ADMIN required for this operation")))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/**").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()

                // ── Read operations: any authenticated user ─────────────────────
                .requestMatchers("GET", "/api/operators/**").authenticated()
                .requestMatchers("GET", "/api/plans/**").authenticated()

                // ── Write operations: ROLE_ADMIN only ───────────────────────────
                // Operators
                .requestMatchers("POST",   "/api/operators/**").hasAuthority("ROLE_ADMIN")
                .requestMatchers("PUT",    "/api/operators/**").hasAuthority("ROLE_ADMIN")
                .requestMatchers("PATCH",  "/api/operators/**").hasAuthority("ROLE_ADMIN")
                .requestMatchers("DELETE", "/api/operators/**").hasAuthority("ROLE_ADMIN")
                // Plans
                .requestMatchers("POST",   "/api/plans/**").hasAuthority("ROLE_ADMIN")
                .requestMatchers("PUT",    "/api/plans/**").hasAuthority("ROLE_ADMIN")
                .requestMatchers("PATCH",  "/api/plans/**").hasAuthority("ROLE_ADMIN")
                .requestMatchers("DELETE", "/api/plans/**").hasAuthority("ROLE_ADMIN")

                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}