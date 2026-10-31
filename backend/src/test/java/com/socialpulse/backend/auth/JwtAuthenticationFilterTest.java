package com.socialpulse.backend.auth;

import com.socialpulse.backend.entity.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.*;

class JwtAuthenticationFilterTest {

    private static final String SECRET = "test-icin-yeterince-uzun-gizli-anahtar-123456";

    private final JwtService jwtService = new JwtService(SECRET, 60);
    private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService);

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private User sampleUser() {
        User u = new User();
        u.setId(7L);
        u.setEmail("ali@example.com");
        return u;
    }

    private MockFilterChain run(String authorizationHeader) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        if (authorizationHeader != null) {
            request.addHeader("Authorization", authorizationHeader);
        }
        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(request, new MockHttpServletResponse(), chain);
        return chain;
    }

    @Test
    void validToken_setsAuthentication() throws Exception {
        MockFilterChain chain = run("Bearer " + jwtService.generateToken(sampleUser()));

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        AuthenticatedUser principal = (AuthenticatedUser) authentication.getPrincipal();
        assertEquals(7L, principal.id());
        assertEquals("ali@example.com", principal.email());
        assertNotNull(chain.getRequest(), "Zincir devam etmeli");
    }

    @Test
    void noHeader_leavesRequestUnauthenticated() throws Exception {
        MockFilterChain chain = run(null);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertNotNull(chain.getRequest());
    }

    @Test
    void nonBearerHeader_isIgnored() throws Exception {
        run("Basic YWxpOnNpZnJl");

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void garbageToken_isIgnored() throws Exception {
        MockFilterChain chain = run("Bearer bu.bir.token.degil");

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertNotNull(chain.getRequest(), "Geçersiz token isteği düşürmemeli, sadece kimlik atanmamalı");
    }

    @Test
    void expiredToken_isIgnored() throws Exception {
        String expired = new JwtService(SECRET, -1).generateToken(sampleUser());

        run("Bearer " + expired);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void emptyBearerToken_isIgnored() throws Exception {
        run("Bearer ");

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
}
