package com.paypal.user_service.security;

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
public class InternalApiKeyFilter extends OncePerRequestFilter {

    private static final String INTERNAL_API_KEY_HEADER = "X-Internal-Api-Key";
    private static final String INTERNAL_USERS_PATH_PREFIX = "/api/users/internal";

    private final String internalApiKey;

    public InternalApiKeyFilter(@Value("${app.security.internal-api-key}") String internalApiKey) {
        this.internalApiKey = internalApiKey;
    }

    @PostConstruct
    void validateConfiguration() {
        if (!StringUtils.hasText(internalApiKey)) {
            throw new IllegalStateException("app.security.internal-api-key must be configured");
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith(INTERNAL_USERS_PATH_PREFIX);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String suppliedKey = request.getHeader(INTERNAL_API_KEY_HEADER);
        if (StringUtils.hasText(suppliedKey) && MessageDigest.isEqual(
                suppliedKey.getBytes(StandardCharsets.UTF_8),
                internalApiKey.getBytes(StandardCharsets.UTF_8)
        )) {
            filterChain.doFilter(request, response);
            return;
        }

        response.sendError(HttpServletResponse.SC_FORBIDDEN, "Internal API key required");
    }
}
