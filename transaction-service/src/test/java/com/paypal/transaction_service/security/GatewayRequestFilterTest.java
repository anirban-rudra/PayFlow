package com.paypal.transaction_service.security;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayRequestFilterTest {

    @Test
    void allowsValidGatewayRequestKey() throws ServletException, IOException {
        GatewayRequestFilter filter = new GatewayRequestFilter("secret");
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/transactions/create");
        request.addHeader("X-Gateway-Request-Key", "secret");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void deniesMissingGatewayRequestKeyForTransactionPaths() throws ServletException, IOException {
        GatewayRequestFilter filter = new GatewayRequestFilter("secret");
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/transactions/create");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(403);
    }

    @Test
    void ignoresNonTransactionPaths() throws ServletException, IOException {
        GatewayRequestFilter filter = new GatewayRequestFilter("secret");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/health");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(200);
    }
}
