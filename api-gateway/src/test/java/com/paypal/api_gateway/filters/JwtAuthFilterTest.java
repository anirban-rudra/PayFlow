package com.paypal.api_gateway.filters;

import com.paypal.api_gateway.util.JwtUtil;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class JwtAuthFilterTest {

    private static final String SECRET = "test-secret-test-secret-test-secret12";

    @Test
    void publicAuthPathsBypassJwtValidation() {
        JwtAuthFilter filter = new JwtAuthFilter(new JwtUtil(SECRET), "test-gateway-request-key");
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.post("/auth/login/"));
        AtomicReference<ServerWebExchange> chainedExchange = new AtomicReference<>();
        GatewayFilterChain chain = request -> {
            chainedExchange.set(request);
            return Mono.empty();
        };

        filter.filter(exchange, chain).block();

        assertThat(chainedExchange.get()).isSameAs(exchange);
        assertThat(exchange.getResponse().getStatusCode()).isNull();
    }

    @Test
    void missingBearerTokenReturnsUnauthorized() {
        JwtAuthFilter filter = new JwtAuthFilter(new JwtUtil(SECRET), "test-gateway-request-key");
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/users/1"));

        filter.filter(exchange, request -> Mono.empty()).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void malformedOrTamperedBearerTokenReturnsUnauthorized() {
        JwtAuthFilter filter = new JwtAuthFilter(new JwtUtil(SECRET), "test-gateway-request-key");
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/users/1")
                .header(HttpHeaders.AUTHORIZATION, "Bearer not-a-real-token"));

        filter.filter(exchange, request -> Mono.empty()).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void authSubpathsBypassJwtValidationForFutureAuthEndpoints() {
        JwtAuthFilter filter = new JwtAuthFilter(new JwtUtil(SECRET), "test-gateway-request-key");
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.post("/auth/password-reset"));
        AtomicReference<ServerWebExchange> chainedExchange = new AtomicReference<>();

        filter.filter(exchange, request -> {
            chainedExchange.set(request);
            return Mono.empty();
        }).block();

        assertThat(chainedExchange.get()).isSameAs(exchange);
        assertThat(exchange.getResponse().getStatusCode()).isNull();
    }

    @Test
    void validJwtRemovesSpoofedHeadersAndAddsTrustedHeaders() {
        JwtAuthFilter filter = new JwtAuthFilter(new JwtUtil(SECRET), "test-gateway-request-key");
        String token = token(42L, "user@example.com", "ROLE_USER");
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/users/42")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .header("X-User-Id", "999")
                .header("X-User-Email", "spoof@example.com")
                .header("X-User-Role", "ROLE_ADMIN")
                .header("X-Gateway-Request-Key", "spoofed-gateway-key")
                .header("X-Internal-Api-Key", "spoofed-internal-key"));
        AtomicReference<ServerWebExchange> chainedExchange = new AtomicReference<>();
        GatewayFilterChain chain = request -> {
            chainedExchange.set(request);
            return Mono.empty();
        };

        filter.filter(exchange, chain).block();

        HttpHeaders headers = chainedExchange.get().getRequest().getHeaders();
        assertThat(headers.getFirst("X-User-Id")).isEqualTo("42");
        assertThat(headers.getFirst("X-User-Email")).isEqualTo("user@example.com");
        assertThat(headers.getFirst("X-User-Role")).isEqualTo("ROLE_USER");
        assertThat(headers.getFirst("X-Gateway-Request-Key")).isEqualTo("test-gateway-request-key");
        assertThat(headers.containsKey("X-Internal-Api-Key")).isFalse();
    }

    @Test
    void reportsHighPrecedenceFilterOrder() {
        assertThat(new JwtAuthFilter(new JwtUtil(SECRET), "test-gateway-request-key").getOrder()).isEqualTo(-100);
    }

    private String token(Long userId, String email, String role) {
        return Jwts.builder()
                .setClaims(Map.of("userId", userId, "role", role))
                .setSubject(email)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)), SignatureAlgorithm.HS256)
                .compact();
    }
}
