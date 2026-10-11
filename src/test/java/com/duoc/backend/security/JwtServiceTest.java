package com.duoc.backend.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private String secreto;
    private JwtService servicio;

    @BeforeEach
    void preparar() {
        byte[] bytes = new byte[48];
        new SecureRandom().nextBytes(bytes);
        secreto = Base64.getEncoder().encodeToString(bytes);
        servicio = new JwtService(secreto, 60000L);
    }

    @Test
    void generaYLeeTokenValido() {
        String token = servicio.generarToken("ana", List.of("ADMIN", "VETERINARIO"));
        Optional<Claims> claims = servicio.leerToken(token);
        assertTrue(claims.isPresent());
        assertEquals("ana", claims.get().getSubject());
        assertEquals(List.of("ADMIN", "VETERINARIO"), claims.get().get("roles", List.class));
        assertEquals(60000L, servicio.getExpiracionMs());
    }

    @Test
    void rechazaTokenAlterado() {
        String token = servicio.generarToken("ana", List.of("ADMIN"));
        String alterado = token.substring(0, token.length() - 10) + "AAAAAAAAAA";
        assertTrue(servicio.leerToken(alterado).isEmpty());
    }

    @Test
    void rechazaTokenExpirado() {
        JwtService expirado = new JwtService(secreto, -1000L);
        assertTrue(servicio.leerToken(expirado.generarToken("ana", List.of("ADMIN"))).isEmpty());
    }

    @Test
    void rechazaTokenConOtroEmisor() {
        SecretKey clave = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secreto));
        String token = Jwts.builder()
                .issuer("otro-sistema")
                .subject("ana")
                .expiration(new Date(System.currentTimeMillis() + 60000))
                .signWith(clave)
                .compact();
        assertTrue(servicio.leerToken(token).isEmpty());
    }

    @Test
    void rechazaTextoQueNoEsToken() {
        assertTrue(servicio.leerToken("no-es-un-token").isEmpty());
        assertTrue(servicio.leerToken("").isEmpty());
    }
}
