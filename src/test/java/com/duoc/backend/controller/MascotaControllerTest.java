package com.duoc.backend.controller;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.duoc.backend.dto.MascotaRequest;
import com.duoc.backend.model.Mascota;
import com.duoc.backend.model.Propietario;
import com.duoc.backend.model.Rol;
import com.duoc.backend.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

class MascotaControllerTest extends IntegrationTestBase {

    private MascotaRequest solicitud(Long propietarioId, Integer edad) {
        return new MascotaRequest("Michi", "Gato", "Siames", edad, propietarioId);
    }

    @Test
    void creaMascota() throws Exception {
        Propietario propietario = crearPropietario();
        enviar(HttpMethod.POST, "/mascotas", Rol.RECEPCIONISTA, solicitud(propietario.getId(), 3))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("Michi"))
                .andExpect(jsonPath("$.propietarioId").value(propietario.getId()));
    }

    @Test
    void rechazaPropietarioInexistente() throws Exception {
        enviar(HttpMethod.POST, "/mascotas", Rol.ADMIN, solicitud(999999L, 3))
                .andExpect(status().isNotFound());
    }

    @Test
    void rechazaEdadNegativa() throws Exception {
        Propietario propietario = crearPropietario();
        enviar(HttpMethod.POST, "/mascotas", Rol.ADMIN, solicitud(propietario.getId(), -1))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listaTodasYPorPropietario() throws Exception {
        Propietario uno = crearPropietario();
        Propietario dos = crearPropietario();
        crearMascota(uno);
        crearMascota(uno);
        crearMascota(dos);
        enviar(HttpMethod.GET, "/mascotas", Rol.VETERINARIO, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));
        enviar(HttpMethod.GET, "/mascotas?propietarioId=" + uno.getId(), Rol.VETERINARIO, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void obtieneMascotaYRetorna404SiNoExiste() throws Exception {
        Mascota mascota = crearMascota(crearPropietario());
        enviar(HttpMethod.GET, "/mascotas/" + mascota.getId(), Rol.RECEPCIONISTA, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.especie").value("Perro"));
        enviar(HttpMethod.GET, "/mascotas/999999", Rol.RECEPCIONISTA, null)
                .andExpect(status().isNotFound());
    }

    @Test
    void actualizaMascota() throws Exception {
        Propietario propietario = crearPropietario();
        Mascota mascota = crearMascota(propietario);
        enviar(HttpMethod.PUT, "/mascotas/" + mascota.getId(), Rol.RECEPCIONISTA, solicitud(propietario.getId(), 4))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Michi"))
                .andExpect(jsonPath("$.edad").value(4));
    }

    @Test
    void eliminaMascota() throws Exception {
        Mascota mascota = crearMascota(crearPropietario());
        enviar(HttpMethod.DELETE, "/mascotas/" + mascota.getId(), Rol.ADMIN, null)
                .andExpect(status().isNoContent());
    }

    @Test
    void noEliminaMascotaConCitas() throws Exception {
        Mascota mascota = crearMascota(crearPropietario());
        crearCita(mascota, crearVeterinario(), fechaFutura());
        enviar(HttpMethod.DELETE, "/mascotas/" + mascota.getId(), Rol.ADMIN, null)
                .andExpect(status().isConflict());
    }
}
