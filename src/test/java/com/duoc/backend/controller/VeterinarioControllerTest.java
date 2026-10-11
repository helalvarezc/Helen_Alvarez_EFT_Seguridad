package com.duoc.backend.controller;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.duoc.backend.dto.VeterinarioRequest;
import com.duoc.backend.model.Rol;
import com.duoc.backend.model.Veterinario;
import com.duoc.backend.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

class VeterinarioControllerTest extends IntegrationTestBase {

    private VeterinarioRequest solicitud(String email) {
        return new VeterinarioRequest("Dra. Mena", "Cirugia", email);
    }

    @Test
    void creaVeterinario() throws Exception {
        enviar(HttpMethod.POST, "/veterinarios", Rol.ADMIN, solicitud("mena@vet.cl"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("Dra. Mena"));
    }

    @Test
    void rechazaCorreoInvalido() throws Exception {
        enviar(HttpMethod.POST, "/veterinarios", Rol.ADMIN, solicitud("no-es-correo"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rechazaCorreoDuplicado() throws Exception {
        Veterinario existente = crearVeterinario();
        enviar(HttpMethod.POST, "/veterinarios", Rol.ADMIN, solicitud(existente.getEmail()))
                .andExpect(status().isConflict());
    }

    @Test
    void listaYObtieneVeterinarios() throws Exception {
        Veterinario veterinario = crearVeterinario();
        enviar(HttpMethod.GET, "/veterinarios", Rol.RECEPCIONISTA, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
        enviar(HttpMethod.GET, "/veterinarios/" + veterinario.getId(), Rol.VETERINARIO, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(veterinario.getEmail()));
        enviar(HttpMethod.GET, "/veterinarios/999999", Rol.VETERINARIO, null)
                .andExpect(status().isNotFound());
    }

    @Test
    void actualizaVeterinario() throws Exception {
        Veterinario veterinario = crearVeterinario();
        enviar(HttpMethod.PUT, "/veterinarios/" + veterinario.getId(), Rol.ADMIN, solicitud(veterinario.getEmail()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.especialidad").value("Cirugia"));
    }

    @Test
    void actualizarConCorreoDeOtroRetorna409() throws Exception {
        Veterinario primero = crearVeterinario();
        Veterinario segundo = crearVeterinario();
        enviar(HttpMethod.PUT, "/veterinarios/" + segundo.getId(), Rol.ADMIN, solicitud(primero.getEmail()))
                .andExpect(status().isConflict());
    }

    @Test
    void eliminaVeterinario() throws Exception {
        Veterinario veterinario = crearVeterinario();
        enviar(HttpMethod.DELETE, "/veterinarios/" + veterinario.getId(), Rol.ADMIN, null)
                .andExpect(status().isNoContent());
    }

    @Test
    void noEliminaVeterinarioConCitas() throws Exception {
        Veterinario veterinario = crearVeterinario();
        crearCita(crearMascota(crearPropietario()), veterinario, fechaFutura());
        enviar(HttpMethod.DELETE, "/veterinarios/" + veterinario.getId(), Rol.ADMIN, null)
                .andExpect(status().isConflict());
    }
}
