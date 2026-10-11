package com.duoc.backend.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String PREFIJO = "Bearer ";

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String cabecera = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (cabecera != null && cabecera.startsWith(PREFIJO)) {
            jwtService.leerToken(cabecera.substring(PREFIJO.length())).ifPresent(claims -> autenticar(claims, request));
        }
        chain.doFilter(request, response);
    }

    private void autenticar(Claims claims, HttpServletRequest request) {
        Object roles = claims.get(JwtService.CLAIM_ROLES);
        List<SimpleGrantedAuthority> autoridades = List.of();
        if (roles instanceof List<?> lista) {
            autoridades = lista.stream().map(rol -> new SimpleGrantedAuthority("ROLE_" + rol)).toList();
        }
        UsernamePasswordAuthenticationToken autenticacion =
                new UsernamePasswordAuthenticationToken(claims.getSubject(), null, autoridades);
        autenticacion.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(autenticacion);
    }
}
