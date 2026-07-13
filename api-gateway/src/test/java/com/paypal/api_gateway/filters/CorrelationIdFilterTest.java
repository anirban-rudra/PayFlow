package com.paypal.api_gateway.filters;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicReference;

import static com.paypal.api_gateway.filters.CorrelationIdFilter.CORRELATION_ID_HEADER;
import static org.assertj.core.api.Assertions.assertThat;

class CorrelationIdFilterTest {

    @Test
    void createsCorrelationIdWhenMissing() {
        CorrelationIdFilter filter = new CorrelationIdFilter();
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/users/1"));
        AtomicReference<ServerWebExchange> chainedExchange = new AtomicReference<>();
        GatewayFilterChain chain = request -> {
            chainedExchange.set(request);
            return Mono.empty();
        };

        filter.filter(exchange, chain).block();

        String requestHeader = chainedExchange.get().getRequest().getHeaders().getFirst(CORRELATION_ID_HEADER);
        assertThat(requestHeader).isNotBlank();
        assertThat(exchange.getResponse().getHeaders().getFirst(CORRELATION_ID_HEADER)).isEqualTo(requestHeader);
    }

    @Test
    void preservesClientCorrelationId() {
        CorrelationIdFilter filter = new CorrelationIdFilter();
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/users/1")
                .header(CORRELATION_ID_HEADER, "client-request-123"));
        AtomicReference<ServerWebExchange> chainedExchange = new AtomicReference<>();

        filter.filter(exchange, request -> {
            chainedExchange.set(request);
            return Mono.empty();
        }).block();

        assertThat(chainedExchange.get().getRequest().getHeaders().getFirst(CORRELATION_ID_HEADER)).isEqualTo("client-request-123");
        assertThat(exchange.getResponse().getHeaders().getFirst(CORRELATION_ID_HEADER)).isEqualTo("client-request-123");
    }

    @Test
    void runsBeforeAuthenticationFilter() {
        assertThat(new CorrelationIdFilter().getOrder()).isLessThan(new JwtAuthFilter(null, "test-gateway-request-key").getOrder());
    }
}
