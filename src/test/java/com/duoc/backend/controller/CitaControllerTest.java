package com.duoc.backend.controller;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.duoc.backend.dto.CitaEstadoRequest;
import com.duoc.backend.dto.CitaRequest;
import com.duoc.backend.model.Cita;
import com.duoc.backend.model.EstadoCita;
import com.duoc.backend.model.Mascota;
import com.duoc.backend.model.Rol;
import com.duoc.backend.model.Veterinario;
import com.duoc.backend.support.IntegrationTestBase;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

class CitaControllerTest extends IntegrationTestBase {

    private Mascota mascota;
    private Veterinario veterinario;

    @BeforeEach
    void preparar() {
        mascota = crearMascota(crearPropietario());
        veterinario = crearVeterinario();
    }

    private CitaRequest solicitud(LocalDateTime fechaHora) {
        return new CitaRequest(fechaHora, "Vacunacion", mascota.getId(), veterinario.getId());
    }

    @Test
    void creaCita() throws Exception {
        enviar(HttpMethod.POST, "/citas", Rol.RECEPCIONISTA, solicitud(fechaFutura()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("PROGRAMADA"))
                .andExpect(jsonPath("$.mascotaNombre").value("Firulais"));
    }

    @Test
    void rechazaFechaPasada() throws Exception {
        enviar(HttpMethod.POST, "/citas", Rol.ADMIN, solicitud(LocalDateTime.now().minusDays(1)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rechazaMascotaOVeterinarioInexistente() throws Exception {
        enviar(HttpMethod.POST, "/citas", Rol.ADMIN,
                new CitaRequest(fechaFutura(), "Control", 999999L, veterinario.getId()))
                .andExpect(status().isNotFound());
        enviar(HttpMethod.POST, "/citas", Rol.ADMIN,
                new CitaRequest(fechaFutura(), "Control", mascota.getId(), 999999L))
                .andExpect(status().isNotFound());
    }

    @Test
    void rechazaHorarioOcupadoDelVeterinario() throws Exception {
        LocalDateTime horario = fechaFutura();
        crearCita(mascota, veterinario, horario);
        enviar(HttpMethod.POST, "/citas", Rol.ADMIN, solicitud(horario))
                .andExpect(status().isConflict());
    }

    @Test
    void permiteReservarHorarioDeCitaCancelada() throws Exception {
        LocalDateTime horario = fechaFutura();
        Cita cancelada = crearCita(mascota, veterinario, horario);
        cancelada.setEstado(EstadoCita.CANCELADA);
        citaRepository.save(cancelada);
        enviar(HttpMethod.POST, "/citas", Rol.ADMIN, solicitud(horario))
                .andExpect(status().isCreated());
    }

    @Test
    void listaPorFechaYObtiene() throws Exception {
        LocalDateTime horario = fechaFutura();
        Cita cita = crearCita(mascota, veterinario, horario);
        crearCita(mascota, veterinario, horario.plusDays(5));
        enviar(HttpMethod.GET, "/citas", Rol.VETERINARIO, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
        enviar(HttpMethod.GET, "/citas?fecha=" + horario.toLocalDate(), Rol.VETERINARIO, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
        enviar(HttpMethod.GET, "/citas/" + cita.getId(), Rol.VETERINARIO, null)
                .andExpect(status().isOk());
        enviar(HttpMethod.GET, "/citas/999999", Rol.VETERINARIO, null)
                .andExpect(status().isNotFound());
    }

    @Test
    void fechaDeFiltroInvalidaRetorna400() throws Exception {
        enviar(HttpMethod.GET, "/citas?fecha=ayer", Rol.ADMIN, null).andExpect(status().isBadRequest());
    }

    @Test
    void actualizaCitaProgramada() throws Exception {
        Cita cita = crearCita(mascota, veterinario, fechaFutura());
        enviar(HttpMethod.PUT, "/citas/" + cita.getId(), Rol.RECEPCIONISTA, solicitud(fechaFutura().plusDays(1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.motivo").value("Vacunacion"));
    }

    @Test
    void noActualizaCitaNoProgramadaNiConHorarioOcupado() throws Exception {
        LocalDateTime horario = fechaFutura();
        Cita primera = crearCita(mascota, veterinario, horario);
        Cita segunda = crearCita(mascota, veterinario, horario.plusHours(2));
        enviar(HttpMethod.PUT, "/citas/" + segunda.getId(), Rol.ADMIN, solicitud(horario))
                .andExpect(status().isConflict());
        primera.setEstado(EstadoCita.COMPLETADA);
        citaRepository.save(primera);
        enviar(HttpMethod.PUT, "/citas/" + primera.getId(), Rol.ADMIN, solicitud(horario.plusDays(1)))
                .andExpect(status().isConflict());
    }

    @Test
    void cambiaEstadoUnaSolaVez() throws Exception {
        Cita cita = crearCita(mascota, veterinario, fechaFutura());
        enviar(HttpMethod.PATCH, "/citas/" + cita.getId() + "/estado", Rol.VETERINARIO,
                new CitaEstadoRequest(EstadoCita.COMPLETADA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("COMPLETADA"));
        enviar(HttpMethod.PATCH, "/citas/" + cita.getId() + "/estado", Rol.VETERINARIO,
                new CitaEstadoRequest(EstadoCita.CANCELADA))
                .andExpect(status().isConflict());
    }

    @Test
    void noPermiteVolverAProgramada() throws Exception {
        Cita cita = crearCita(mascota, veterinario, fechaFutura());
        enviar(HttpMethod.PATCH, "/citas/" + cita.getId() + "/estado", Rol.ADMIN,
                new CitaEstadoRequest(EstadoCita.PROGRAMADA))
                .andExpect(status().isConflict());
    }

    @Test
    void eliminaCita() throws Exception {
        Cita cita = crearCita(mascota, veterinario, fechaFutura());
        enviar(HttpMethod.DELETE, "/citas/" + cita.getId(), Rol.ADMIN, null)
                .andExpect(status().isNoContent());
    }
}
