package com.eaglebank.service;

import com.eaglebank.security.JwtService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JwtServiceTest {
    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
    }

    @Test
    void testGenerateAndExtractEmail() {
        String email = "alice@example.com";
        String token = jwtService.generateToken(email);

        assertNotNull(token, "Token should not be null");
        assertFalse(token.isBlank(), "Token should not be blank");

        String extractedEmail = jwtService.extractEmail(token);
        assertEquals(email, extractedEmail, "Extracted email should match original email");
    }

    @Test
    void testIsTokenValid_withValidTokenAndMatchingUserDetails_returnsTrue() {
        String email = "alice@example.com";
        String token = jwtService.generateToken(email);

        UserDetails userDetails = mock(UserDetails.class);
        when(userDetails.getUsername()).thenReturn(email);

        boolean isValid = jwtService.isTokenValid(token, userDetails);
        assertTrue(isValid, "Token should be valid");
    }

    @Test
    void testIsTokenValid_withMismatchedUserDetails_returnsFalse() {
        String email = "alice@example.com";
        String token = jwtService.generateToken(email);

        UserDetails userDetails = mock(UserDetails.class);
        when(userDetails.getUsername()).thenReturn("bob@example.com");

        boolean isValid = jwtService.isTokenValid(token, userDetails);
        assertFalse(isValid, "Token should be invalid for mismatched user");
    }

    @Test
    void testIsTokenValid_withMalformedToken_returnsFalse() {
        String malformedToken = "not.a.valid.jwt";

        UserDetails userDetails = mock(UserDetails.class);
        when(userDetails.getUsername()).thenReturn("any@example.com");

        boolean isValid = jwtService.isTokenValid(malformedToken, userDetails);
        assertFalse(isValid, "Malformed token should be invalid");
    }

    @Test
    void testIsTokenExpired_withExpiredToken_returnsTrue() {
        String expiredToken = Jwts.builder()
                .setSubject("expired@example.com")
                .setIssuedAt(new Date(System.currentTimeMillis() - 2 * 86400000L)) // issued 2 days ago
                .setExpiration(new Date(System.currentTimeMillis() - 86400000L))   // expired 1 day ago
                .signWith(jwtService.getKey(), SignatureAlgorithm.HS256)
                .compact();

        UserDetails userDetails = mock(UserDetails.class);
        when(userDetails.getUsername()).thenReturn("expired@example.com");

        boolean isValid = jwtService.isTokenValid(expiredToken, userDetails);
        assertFalse(isValid, "Expired token should be invalid");
    }
}
