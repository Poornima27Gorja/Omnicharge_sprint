package com.omnicharge.operator_service.config;

import com.omnicharge.operator_service.filter.JwtAuthFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	@Autowired
	private JwtAuthFilter jwtAuthFilter;

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http.csrf(csrf -> csrf.disable())
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.exceptionHandling(ex -> ex
						.authenticationEntryPoint((request, response, authException) -> response
								.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized"))
						.accessDeniedHandler((request, response, accessDeniedException) -> response
								.sendError(HttpServletResponse.SC_FORBIDDEN, "Forbidden")))
				.authorizeHttpRequests(auth -> auth.requestMatchers("/actuator/**").permitAll()
						.requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
						// View operators and plans - any authenticated user
						.requestMatchers("GET", "/api/operators/**").authenticated()
						.requestMatchers("GET", "/api/plans/**").authenticated()
						// Add, update, delete - ROLE_ADMIN only
						.requestMatchers("POST", "/api/operators/**").hasAuthority("ROLE_ADMIN")
						.requestMatchers("PUT", "/api/operators/**").hasAuthority("ROLE_ADMIN")
						.requestMatchers("DELETE", "/api/operators/**").hasAuthority("ROLE_ADMIN")
						.requestMatchers("POST", "/api/plans/**").hasAuthority("ROLE_ADMIN")
						.requestMatchers("PUT", "/api/plans/**").hasAuthority("ROLE_ADMIN")
						.requestMatchers("DELETE", "/api/plans/**").hasAuthority("ROLE_ADMIN").anyRequest()
						.authenticated())
				.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}
}
