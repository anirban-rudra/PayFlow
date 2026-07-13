package com.paypal.api_gateway.filters;

import com.paypal.api_gateway.util.JwtUtil;
import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;


@Component
public class JwtAuthFilter implements GlobalFilter, Ordered {
    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);

    private final JwtUtil jwtUtil;
    private final String gatewayRequestKey;

    private static final List<String> PUBLIC_PATHS = List.of(
            "/auth/signup",
            "/auth/login"
    );

    public JwtAuthFilter(JwtUtil jwtUtil,
                         @Value("${app.security.gateway-request-key}") String gatewayRequestKey) {
        this.jwtUtil = jwtUtil;
        this.gatewayRequestKey = gatewayRequestKey;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain){
        String path = exchange.getRequest().getPath().value();
        String normalizedPath = path.replaceAll("/+$", "");

        if (PUBLIC_PATHS.contains(normalizedPath) || normalizedPath.startsWith("/auth/")) {
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.debug("Missing or invalid Authorization header for {}", normalizedPath);
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        try {
            String token = authHeader.substring(7);
            Claims claims = jwtUtil.validateToken(token);

            ServerWebExchange mutatedExchange = exchange.mutate()
                    .request(exchange.getRequest().mutate()
                            .headers(headers -> {
                                headers.remove("X-Internal-Api-Key");
                                headers.remove("X-User-Email");
                                headers.remove("X-User-Id");
                                headers.remove("X-User-Role");
                                headers.remove("X-Gateway-Request-Key");
                                headers.set("X-User-Email", claims.getSubject());
                                headers.set("X-User-Id", String.valueOf(claims.get("userId")));
                                headers.set("X-User-Role", (String) claims.get("role"));
                                headers.set("X-Gateway-Request-Key", gatewayRequestKey);
                            })
                            .build())
                    .build();

            return chain.filter(mutatedExchange);

        } catch (Exception e) {
            log.debug("JWT validation failed for {}: {}", normalizedPath, e.getMessage());
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
