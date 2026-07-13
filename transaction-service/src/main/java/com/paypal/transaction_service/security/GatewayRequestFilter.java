package com.paypal.transaction_service.security;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Component
public class GatewayRequestFilter extends OncePerRequestFilter {

    private static final String GATEWAY_REQUEST_KEY_HEADER = "X-Gateway-Request-Key";
    private static final String TRANSACTION_PATH_PREFIX = "/api/transactions";

    private final String gatewayRequestKey;

    public GatewayRequestFilter(@Value("${app.security.gateway-request-key}") String gatewayRequestKey) {
        this.gatewayRequestKey = gatewayRequestKey;
    }

    @PostConstruct
    void validateConfiguration() {
        if (!StringUtils.hasText(gatewayRequestKey)) {
            throw new IllegalStateException("app.security.gateway-request-key must be configured");
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith(TRANSACTION_PATH_PREFIX);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String suppliedKey = request.getHeader(GATEWAY_REQUEST_KEY_HEADER);
        if (StringUtils.hasText(suppliedKey) && MessageDigest.isEqual(
                suppliedKey.getBytes(StandardCharsets.UTF_8),
                gatewayRequestKey.getBytes(StandardCharsets.UTF_8)
        )) {
            filterChain.doFilter(request, response);
            return;
        }

        response.sendError(HttpServletResponse.SC_FORBIDDEN, "Gateway request key required");
    }
}
