package com.socialpulse.backend.auth;

import com.socialpulse.backend.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private static final String SECRET = "test-icin-yeterince-uzun-gizli-anahtar-123456";

    private User sampleUser() {
        User u = new User();
        u.setId(7L);
        u.setEmail("ali@example.com");
        return u;
    }

    @Test
    void generatedToken_canBeParsed() {
        JwtService service = new JwtService(SECRET, 60);

        Claims claims = service.parse(service.generateToken(sampleUser()));

        assertEquals("7", claims.getSubject());
        assertEquals("ali@example.com", claims.get("email", String.class));
    }

    @Test
    void tamperedToken_isRejected() {
        JwtService service = new JwtService(SECRET, 60);
        String token = service.generateToken(sampleUser());

        assertThrows(JwtException.class, () -> service.parse(token + "x"));
    }

    @Test
    void tokenSignedWithOtherKey_isRejected() {
        String token = new JwtService(SECRET, 60).generateToken(sampleUser());
        JwtService other = new JwtService("baska-bir-gizli-anahtar-en-az-32-karakter-0000", 60);

        assertThrows(JwtException.class, () -> other.parse(token));
    }

    @Test
    void expiredToken_isRejected() {
        JwtService service = new JwtService(SECRET, -1);
        String token = service.generateToken(sampleUser());

        assertThrows(JwtException.class, () -> service.parse(token));
    }
}
