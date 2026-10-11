package com.duoc.backend.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.duoc.backend.dto.LoginRequest;
import com.duoc.backend.model.Rol;
import com.duoc.backend.model.Usuario;
import com.duoc.backend.security.JwtService;
import com.duoc.backend.support.IntegrationTestBase;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.security.SecureRandom;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

class AuthIntegrationTest extends IntegrationTestBase {

    private static final String CLAVE = "Clave#Segura1";

    private String nuevoUsuario() {
        return "u" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    private void crearUsuario(String username, boolean habilitado) {
        Usuario usuario = new Usuario();
        usuario.setUsername(username);
        usuario.setEmail(username + "@vet.cl");
        usuario.setPassword(passwordEncoder.encode(CLAVE));
        usuario.setRol(Rol.RECEPCIONISTA);
        usuario.setEnabled(habilitado);
        usuarioRepository.save(usuario);
    }

    private ResultActions login(String username, String password) throws Exception {
        return mockMvc.perform(post("/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(new LoginRequest(username, password))));
    }

    @Test
    void loginExitosoEntregaTokenUtilizable() throws Exception {
        String username = nuevoUsuario();
        crearUsuario(username, true);

        String respuesta = login(username, CLAVE)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.tipo").value("Bearer"))
                .andReturn().getResponse().getContentAsString();

        String token = objectMapper.readTree(respuesta).get("token").asText();
        mockMvc.perform(get("/propietarios").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void loginConPasswordIncorrectoRetorna401() throws Exception {
        String username = nuevoUsuario();
        crearUsuario(username, true);
        login(username, "Incorrecta#1").andExpect(status().isUnauthorized());
    }

    @Test
    void loginConUsuarioInexistenteRetorna401() throws Exception {
        login(nuevoUsuario(), CLAVE).andExpect(status().isUnauthorized());
    }

    @Test
    void loginConUsuarioDeshabilitadoRetorna401() throws Exception {
        String username = nuevoUsuario();
        crearUsuario(username, false);
        login(username, CLAVE).andExpect(status().isUnauthorized());
    }

    @Test
    void loginSeBloqueaTrasCincoIntentosFallidos() throws Exception {
        String username = nuevoUsuario();
        crearUsuario(username, true);
        for (int i = 0; i < 5; i++) {
            login(username, "Incorrecta#1").andExpect(status().isUnauthorized());
        }
        login(username, CLAVE).andExpect(status().isTooManyRequests());
    }

    @Test
    void loginConCuerpoInvalidoRetorna400() throws Exception {
        login("", "").andExpect(status().isBadRequest());
        mockMvc.perform(post("/login").contentType(MediaType.APPLICATION_JSON).content("no es json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void solicitudSinTokenRetorna401ConCuerpoJson() throws Exception {
        mockMvc.perform(get("/propietarios"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void tokenMalformadoRetorna401() throws Exception {
        mockMvc.perform(get("/propietarios").header(HttpHeaders.AUTHORIZATION, "Bearer abc.def.ghi"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenFirmadoConOtraClaveRetorna401() throws Exception {
        byte[] bytes = new byte[48];
        new SecureRandom().nextBytes(bytes);
        SecretKey otraClave = Keys.hmacShaKeyFor(bytes);
        String token = Jwts.builder()
                .issuer("veterinaria-backend")
                .subject("intruso")
                .claim("roles", List.of("ADMIN"))
                .expiration(new Date(System.currentTimeMillis() + 60000))
                .signWith(otraClave)
                .compact();
        mockMvc.perform(get("/usuarios").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenExpiradoRetorna401() throws Exception {
        JwtService servicioExpirado = new JwtService(secretoJwt(), -1000L);
        String token = servicioExpirado.generarToken("admin", List.of("ADMIN"));
        mockMvc.perform(get("/usuarios").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenSinRolesNoOtorgaAcceso() throws Exception {
        SecretKey clave = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretoJwt()));
        String token = Jwts.builder()
                .issuer("veterinaria-backend")
                .subject("sinroles")
                .expiration(new Date(System.currentTimeMillis() + 60000))
                .signWith(clave)
                .compact();
        mockMvc.perform(get("/propietarios").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void respuestaIncluyeCabecerasDeSeguridad() throws Exception {
        enviar(HttpMethod.GET, "/propietarios", Rol.ADMIN, null)
                .andExpect(status().isOk())
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("Referrer-Policy", "no-referrer"))
                .andExpect(header().string("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none'"));
    }

    @Test
    void rutaDesconocidaEstaDenegada() throws Exception {
        enviar(HttpMethod.GET, "/ruta-inexistente", Rol.ADMIN, null)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }
}
