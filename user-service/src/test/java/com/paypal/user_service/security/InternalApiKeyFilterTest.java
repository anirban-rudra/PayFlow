package com.paypal.user_service.security;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class InternalApiKeyFilterTest {

    @Test
    void allowsValidInternalApiKeyForInternalUserEndpoint() throws ServletException, IOException {
        InternalApiKeyFilter filter = new InternalApiKeyFilter("secret");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users/internal/resolve-pay-tag");
        request.addHeader("X-Internal-Api-Key", "secret");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void deniesMissingInternalApiKeyForInternalUserEndpoint() throws ServletException, IOException {
        InternalApiKeyFilter filter = new InternalApiKeyFilter("secret");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users/internal/resolve-pay-tag");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(403);
    }

    @Test
    void ignoresPublicUserEndpoints() throws ServletException, IOException {
        InternalApiKeyFilter filter = new InternalApiKeyFilter("secret");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users/1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(200);
    }
}
