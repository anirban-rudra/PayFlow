package com.paypal.user_service.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JWTUtilTest {

    private static final String SECRET = "01234567890123456789012345678901";

    @Test
    void validatesConfigurationRequiresStrongHs256Secret() {
        JWTUtil jwtUtil = new JWTUtil("too-short", 60_000);

        assertThatThrownBy(jwtUtil::validateConfiguration)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("jwt.secret must be at least 32 bytes for HS256");
    }

    @Test
    void generatedTokenContainsExpectedSubjectUserIdAndRole() {
        JWTUtil jwtUtil = new JWTUtil(SECRET, 60_000);
        jwtUtil.validateConfiguration();

        String token = jwtUtil.generateToken(10L, "ani@example.com", "ROLE_USER");

        assertThat(jwtUtil.extractUsername(token)).isEqualTo("ani@example.com");
        assertThat(jwtUtil.extractUserId(token)).isEqualTo(10L);
        assertThat(jwtUtil.extractRole(token)).isEqualTo("ROLE_USER");
        assertThat(jwtUtil.validateToken(token, "ani@example.com")).isTrue();
        assertThat(jwtUtil.validateToken(token, "other@example.com")).isFalse();
    }

    @Test
    void expiredTokenIsRejected() {
        JWTUtil jwtUtil = new JWTUtil(SECRET, -1);
        jwtUtil.validateConfiguration();

        String token = jwtUtil.generateToken(10L, "ani@example.com", "ROLE_USER");

        assertThat(jwtUtil.validateToken(token, "ani@example.com")).isFalse();
    }

    @Test
    void tamperedTokenIsRejected() {
        JWTUtil jwtUtil = new JWTUtil(SECRET, 60_000);
        jwtUtil.validateConfiguration();
        String token = jwtUtil.generateToken(10L, "ani@example.com", "ROLE_USER");
        String tampered = token.substring(0, token.length() - 2) + "xx";

        assertThat(jwtUtil.validateToken(tampered, "ani@example.com")).isFalse();
    }
}
