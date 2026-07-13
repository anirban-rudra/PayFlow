package com.paypal.wallet_service.security;

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
public class WalletAccessFilter extends OncePerRequestFilter {

    private static final String INTERNAL_API_KEY_HEADER = "X-Internal-Api-Key";
    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String WALLET_PATH_PREFIX = "/api/v1/wallets";

    private final String internalApiKey;

    public WalletAccessFilter(@Value("${app.security.internal-api-key}") String internalApiKey) {
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
        return !request.getRequestURI().startsWith(WALLET_PATH_PREFIX);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (hasValidInternalApiKey(request) || isOwnerWalletRead(request) || isOwnerTopUp(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        response.sendError(HttpServletResponse.SC_FORBIDDEN, "Wallet access denied");
    }

    private boolean hasValidInternalApiKey(HttpServletRequest request) {
        String suppliedKey = request.getHeader(INTERNAL_API_KEY_HEADER);
        if (!StringUtils.hasText(suppliedKey)) {
            return false;
        }

        return MessageDigest.isEqual(
                suppliedKey.getBytes(StandardCharsets.UTF_8),
                internalApiKey.getBytes(StandardCharsets.UTF_8)
        );
    }

    private boolean isOwnerWalletRead(HttpServletRequest request) {
        if (!"GET".equalsIgnoreCase(request.getMethod())) {
            return false;
        }

        Long requestedUserId = parseUserIdFromWalletPath(request.getRequestURI(), false);
        Long authenticatedUserId = parseLong(request.getHeader(USER_ID_HEADER));
        return requestedUserId != null && requestedUserId.equals(authenticatedUserId);
    }

    private boolean isOwnerTopUp(HttpServletRequest request) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return false;
        }

        Long requestedUserId = parseUserIdFromWalletPath(request.getRequestURI(), true);
        Long authenticatedUserId = parseLong(request.getHeader(USER_ID_HEADER));
        return requestedUserId != null && requestedUserId.equals(authenticatedUserId);
    }

    private Long parseUserIdFromWalletPath(String requestUri, boolean topUpPath) {
        String suffix = requestUri.substring(WALLET_PATH_PREFIX.length());
        if (!suffix.startsWith("/") || suffix.length() <= 1) {
            return null;
        }

        String[] parts = suffix.substring(1).split("/");
        if (!topUpPath && parts.length != 1) {
            return null;
        }
        if (topUpPath && (parts.length != 2 || !"top-ups".equals(parts[1]))) {
            return null;
        }

        return parseLong(parts[0]);
    }

    private Long parseLong(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }

        try {
            return Long.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
