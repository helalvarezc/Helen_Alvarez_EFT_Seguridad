package com.duoc.backend.controller;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.duoc.backend.dto.PropietarioRequest;
import com.duoc.backend.model.Propietario;
import com.duoc.backend.model.Rol;
import com.duoc.backend.support.IntegrationTestBase;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

class PropietarioControllerTest extends IntegrationTestBase {

    private PropietarioRequest solicitud(String rut) {
        return new PropietarioRequest("Luis", "Rojas", rut, "luis@correo.cl", "+56922223333", "Av. Central 123");
    }

    @Test
    void creaPropietario() throws Exception {
        enviar(HttpMethod.POST, "/propietarios", Rol.RECEPCIONISTA, solicitud("12345678-9"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.nombre").value("Luis"))
                .andExpect(jsonPath("$.rut").value("12345678-9"));
    }

    @Test
    void rechazaDatosInvalidos() throws Exception {
        enviar(HttpMethod.POST, "/propietarios", Rol.ADMIN, solicitud("rut-malo"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rechazaCamposDesconocidos() throws Exception {
        enviar(HttpMethod.POST, "/propietarios", Rol.ADMIN, Map.of("nombre", "A", "admin", true))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rechazaRutDuplicado() throws Exception {
        Propietario existente = crearPropietario();
        enviar(HttpMethod.POST, "/propietarios", Rol.ADMIN, solicitud(existente.getRut()))
                .andExpect(status().isConflict());
    }

    @Test
    void obtienePropietarioExistente() throws Exception {
        Propietario propietario = crearPropietario();
        enviar(HttpMethod.GET, "/propietarios/" + propietario.getId(), Rol.VETERINARIO, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rut").value(propietario.getRut()));
    }

    @Test
    void retorna404SiNoExiste() throws Exception {
        enviar(HttpMethod.GET, "/propietarios/999999", Rol.ADMIN, null)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("Propietario no encontrado"));
    }

    @Test
    void parametroDeRutaInvalidoRetorna400() throws Exception {
        enviar(HttpMethod.GET, "/propietarios/abc", Rol.ADMIN, null).andExpect(status().isBadRequest());
    }

    @Test
    void listaPropietarios() throws Exception {
        crearPropietario();
        crearPropietario();
        enviar(HttpMethod.GET, "/propietarios", Rol.RECEPCIONISTA, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void actualizaPropietario() throws Exception {
        Propietario propietario = crearPropietario();
        enviar(HttpMethod.PUT, "/propietarios/" + propietario.getId(), Rol.RECEPCIONISTA, solicitud(propietario.getRut()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Luis"));
    }

    @Test
    void actualizarConRutDeOtroPropietarioRetorna409() throws Exception {
        Propietario primero = crearPropietario();
        Propietario segundo = crearPropietario();
        enviar(HttpMethod.PUT, "/propietarios/" + segundo.getId(), Rol.ADMIN, solicitud(primero.getRut()))
                .andExpect(status().isConflict());
    }

    @Test
    void actualizarInexistenteRetorna404() throws Exception {
        enviar(HttpMethod.PUT, "/propietarios/999999", Rol.ADMIN, solicitud("12345678-9"))
                .andExpect(status().isNotFound());
    }

    @Test
    void eliminaPropietario() throws Exception {
        Propietario propietario = crearPropietario();
        enviar(HttpMethod.DELETE, "/propietarios/" + propietario.getId(), Rol.ADMIN, null)
                .andExpect(status().isNoContent());
    }

    @Test
    void noEliminaPropietarioConMascotas() throws Exception {
        Propietario propietario = crearPropietario();
        crearMascota(propietario);
        enviar(HttpMethod.DELETE, "/propietarios/" + propietario.getId(), Rol.ADMIN, null)
                .andExpect(status().isConflict());
    }
}
