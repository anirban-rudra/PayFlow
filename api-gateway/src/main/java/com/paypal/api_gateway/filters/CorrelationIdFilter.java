package com.paypal.api_gateway.filters;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
public class CorrelationIdFilter implements GlobalFilter, Ordered {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    private static final Logger log = LoggerFactory.getLogger(CorrelationIdFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String incomingCorrelationId = exchange.getRequest().getHeaders().getFirst(CORRELATION_ID_HEADER);
        if (!StringUtils.hasText(incomingCorrelationId)) {
            incomingCorrelationId = UUID.randomUUID().toString();
        }
        String correlationId = incomingCorrelationId;

        ServerHttpRequest request = exchange.getRequest().mutate()
                .headers(headers -> headers.set(CORRELATION_ID_HEADER, correlationId))
                .build();
        exchange.getResponse().getHeaders().set(CORRELATION_ID_HEADER, correlationId);

        String requestId = correlationId;
        return chain.filter(exchange.mutate().request(request).build())
                .doOnError(error -> log.warn("Request failed correlationId={}", requestId, error));
    }

    @Override
    public int getOrder() {
        return -200;
    }
}
