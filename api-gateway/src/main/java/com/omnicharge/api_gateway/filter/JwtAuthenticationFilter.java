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

// GlobalFilter runs on every request passing through the gateway
@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    @Autowired
    private JwtUtil jwtUtil;

    // Public endpoints that do not need JWT validation
    private static final List<String> PUBLIC_ENDPOINTS = List.of(
            "/api/auth/register",
            "/api/auth/register-admin",
            "/api/auth/login",
            "/actuator"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // Allow public endpoints to pass through without token
        if (isPublicEndpoint(path)) {
            return chain.filter(exchange);
        }

        // Check Authorization header exists
        if (!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
            return unauthorizedResponse(exchange, "Missing Authorization header");
        }

        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return unauthorizedResponse(exchange, "Invalid Authorization header format");
        }

        String token = authHeader.substring(7);

        // Validate JWT token
        if (!jwtUtil.validateToken(token)) {
            return unauthorizedResponse(exchange, "Invalid or expired token");
        }

        // Extract user info and add to request headers for downstream services
        String username = jwtUtil.extractUsername(token);
        String role = jwtUtil.extractRole(token);

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

    // Run this filter before all other filters
    @Override
    public int getOrder() {
        return -1;
    }
}
