package com.socialpulse.backend.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class RestAuthenticationEntryPointTest {

    @Test
    void commence_returns401WithJsonBody() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        new RestAuthenticationEntryPoint().commence(
                new MockHttpServletRequest(), response, new BadCredentialsException("test"));

        assertEquals(401, response.getStatus());
        assertTrue(response.getContentType().startsWith("application/problem+json"));
        String body = response.getContentAsString(StandardCharsets.UTF_8);
        assertTrue(body.contains("\"status\":401"));
        assertTrue(body.contains("giriş yapmanız gerekiyor"));
    }
}
