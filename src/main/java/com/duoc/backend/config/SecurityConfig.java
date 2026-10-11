package com.duoc.backend.config;

import com.duoc.backend.security.JwtAuthenticationFilter;
import com.duoc.backend.security.JwtService;
import com.duoc.backend.security.RestAccessDeniedHandler;
import com.duoc.backend.security.RestAuthenticationEntryPoint;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String ADMIN = "ADMIN";
    private static final String VETERINARIO = "VETERINARIO";
    private static final String RECEPCIONISTA = "RECEPCIONISTA";

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtService jwtService,
                                                   RestAuthenticationEntryPoint entryPoint,
                                                   RestAccessDeniedHandler accessDeniedHandler,
                                                   CorsConfigurationSource corsConfigurationSource) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .headers(headers -> headers
                        .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'none'; frame-ancestors 'none'"))
                        .frameOptions(HeadersConfigurer.FrameOptionsConfig::deny)
                        .referrerPolicy(referrer -> referrer.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER)))
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/error").permitAll()
                        .requestMatchers(HttpMethod.POST, "/login").permitAll()
                        .requestMatchers("/usuarios/**").hasRole(ADMIN)
                        .requestMatchers(HttpMethod.DELETE, "/**").hasRole(ADMIN)
                        .requestMatchers("/registros-medicos/**").hasAnyRole(ADMIN, VETERINARIO)
                        .requestMatchers("/facturas/**").hasAnyRole(ADMIN, RECEPCIONISTA)
                        .requestMatchers(HttpMethod.GET, "/propietarios/**", "/mascotas/**", "/veterinarios/**", "/citas/**")
                        .hasAnyRole(ADMIN, RECEPCIONISTA, VETERINARIO)
                        .requestMatchers(HttpMethod.PATCH, "/citas/**").hasAnyRole(ADMIN, RECEPCIONISTA, VETERINARIO)
                        .requestMatchers(HttpMethod.POST, "/propietarios/**", "/mascotas/**", "/citas/**")
                        .hasAnyRole(ADMIN, RECEPCIONISTA)
                        .requestMatchers(HttpMethod.PUT, "/propietarios/**", "/mascotas/**", "/citas/**")
                        .hasAnyRole(ADMIN, RECEPCIONISTA)
                        .requestMatchers(HttpMethod.POST, "/veterinarios/**").hasRole(ADMIN)
                        .requestMatchers(HttpMethod.PUT, "/veterinarios/**").hasRole(ADMIN)
                        .anyRequest().denyAll())
                .addFilterBefore(new JwtAuthenticationFilter(jwtService), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(@Value("${app.cors.allowed-origins}") List<String> origenes) {
        CorsConfiguration configuracion = new CorsConfiguration();
        configuracion.setAllowedOrigins(origenes);
        configuracion.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE"));
        configuracion.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        configuracion.setAllowCredentials(false);
        configuracion.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource fuente = new UrlBasedCorsConfigurationSource();
        fuente.registerCorsConfiguration("/**", configuracion);
        return fuente;
    }
}
