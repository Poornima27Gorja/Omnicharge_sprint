package com.omnicharge.api_gateway.filter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * Public endpoints — JWT validation is SKIPPED for these paths.
     *
     * Includes:
     *   /api/auth/register         — user registration
     *   /api/auth/register-admin   — admin registration (secret-key protected internally)
     *   /api/auth/login            — user + admin standard login
     *   /api/auth/admin/login      — admin-portal dedicated login (rejects non-admins)
     *   /actuator                  — health checks
     *   /swagger-ui, /v3/api-docs  — Swagger UI
     */
    private static final List<String> PUBLIC_ENDPOINTS = List.of(
        "/api/auth/register",
        "/api/auth/register-admin",
        "/api/auth/login",
        "/api/auth/admin/login",
        "/actuator",
        "/swagger-ui",
        "/v3/api-docs",
        "/webjars",
        "/user-service/v3/api-docs",
        "/operator-service/v3/api-docs",
        "/recharge-service/v3/api-docs",
        "/payment-service/v3/api-docs"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        if (isPublicEndpoint(path)) {
            return chain.filter(exchange);
        }

        if (!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
            return unauthorizedResponse(exchange, "Missing Authorization header");
        }

        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return unauthorizedResponse(exchange, "Invalid Authorization header format. Use: Bearer <token>");
        }

        String token = authHeader.substring(7);

        if (!jwtUtil.validateToken(token)) {
            return unauthorizedResponse(exchange, "Invalid or expired token");
        }

        String username = jwtUtil.extractUsername(token);
        String role     = jwtUtil.extractRole(token);

        // Forward user identity to downstream services as headers
        // Downstream services read X-Auth-Username and X-Auth-Role
        ServerHttpRequest mutatedRequest = request.mutate()
            .header("X-Auth-Username", username)
            .header("X-Auth-Role", role)
            .build();

        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    private boolean isPublicEndpoint(String path) {
        return PUBLIC_ENDPOINTS.stream().anyMatch(path::startsWith);
    }

    private Mono<Void> unauthorizedResponse(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().add("Content-Type", "application/json");
        var buffer = response.bufferFactory()
            .wrap(("{\"error\": \"" + message + "\"}").getBytes());
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -1;
    }
}