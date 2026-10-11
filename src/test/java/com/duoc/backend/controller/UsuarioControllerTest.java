package com.duoc.backend.controller;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.duoc.backend.dto.UsuarioRequest;
import com.duoc.backend.model.Rol;
import com.duoc.backend.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

class UsuarioControllerTest extends IntegrationTestBase {

    private static final String CLAVE = "Clave#Segura1";

    @Test
    void adminCreaUsuarioSinExponerPassword() throws Exception {
        UsuarioRequest solicitud = new UsuarioRequest("nuevo.usuario", "nuevo@vet.cl", CLAVE, Rol.VETERINARIO);
        enviar(HttpMethod.POST, "/usuarios", Rol.ADMIN, solicitud)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("nuevo.usuario"))
                .andExpect(jsonPath("$.rol").value("VETERINARIO"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void almacenaPasswordCifrada() throws Exception {
        UsuarioRequest solicitud = new UsuarioRequest("cifrado.user", "cifrado@vet.cl", CLAVE, Rol.RECEPCIONISTA);
        enviar(HttpMethod.POST, "/usuarios", Rol.ADMIN, solicitud).andExpect(status().isCreated());
        String guardada = usuarioRepository.findByUsername("cifrado.user").orElseThrow().getPassword();
        assertNotEquals(CLAVE, guardada);
        assertTrue(passwordEncoder.matches(CLAVE, guardada));
    }

    @Test
    void rechazaPasswordDebil() throws Exception {
        UsuarioRequest solicitud = new UsuarioRequest("debil.user", "debil@vet.cl", "123456", Rol.VETERINARIO);
        enviar(HttpMethod.POST, "/usuarios", Rol.ADMIN, solicitud).andExpect(status().isBadRequest());
    }

    @Test
    void rechazaUsuarioYCorreoDuplicados() throws Exception {
        UsuarioRequest original = new UsuarioRequest("dup.user", "dup@vet.cl", CLAVE, Rol.VETERINARIO);
        enviar(HttpMethod.POST, "/usuarios", Rol.ADMIN, original).andExpect(status().isCreated());
        enviar(HttpMethod.POST, "/usuarios", Rol.ADMIN,
                new UsuarioRequest("dup.user", "otro@vet.cl", CLAVE, Rol.VETERINARIO))
                .andExpect(status().isConflict());
        enviar(HttpMethod.POST, "/usuarios", Rol.ADMIN,
                new UsuarioRequest("otro.user", "dup@vet.cl", CLAVE, Rol.VETERINARIO))
                .andExpect(status().isConflict());
    }

    @Test
    void adminListaUsuarios() throws Exception {
        enviar(HttpMethod.POST, "/usuarios", Rol.ADMIN,
                new UsuarioRequest("lista.user", "lista@vet.cl", CLAVE, Rol.RECEPCIONISTA))
                .andExpect(status().isCreated());
        enviar(HttpMethod.GET, "/usuarios", Rol.ADMIN, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }
}
