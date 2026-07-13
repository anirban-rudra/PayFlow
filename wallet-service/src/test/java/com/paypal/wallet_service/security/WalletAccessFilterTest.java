package com.paypal.wallet_service.security;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WalletAccessFilterTest {

    @Test
    void validatesInternalApiKeyIsConfigured() {
        WalletAccessFilter filter = new WalletAccessFilter(" ");

        assertThatThrownBy(filter::validateConfiguration)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("app.security.internal-api-key must be configured");
    }

    @Test
    void skipsNonWalletPaths() throws ServletException, IOException {
        WalletAccessFilter filter = new WalletAccessFilter("secret");
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/actuator/health");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void validInternalApiKeyAllowsWalletMutation() throws ServletException, IOException {
        WalletAccessFilter filter = new WalletAccessFilter("secret");
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/wallets/10/debit");
        request.addHeader("X-Internal-Api-Key", "secret");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void ownerCanReadOwnWalletWithoutInternalApiKey() throws ServletException, IOException {
        WalletAccessFilter filter = new WalletAccessFilter("secret");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/wallets/10");
        request.addHeader("X-User-Id", "10");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void ownerCanTopUpOwnWalletWithoutInternalApiKey() throws ServletException, IOException {
        WalletAccessFilter filter = new WalletAccessFilter("secret");
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/wallets/10/top-ups");
        request.addHeader("X-User-Id", "10");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void deniesCrossUserTopUpWithoutInternalApiKey() throws ServletException, IOException {
        WalletAccessFilter filter = new WalletAccessFilter("secret");
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/wallets/10/top-ups");
        request.addHeader("X-User-Id", "99");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(403);
    }

    @Test
    void deniesInvalidKeyAndCrossUserAccess() throws ServletException, IOException {
        WalletAccessFilter filter = new WalletAccessFilter("secret");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/wallets/10");
        request.addHeader("X-Internal-Api-Key", "wrong");
        request.addHeader("X-User-Id", "99");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getErrorMessage()).isEqualTo("Wallet access denied");
    }
}
