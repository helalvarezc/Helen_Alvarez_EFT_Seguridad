package com.duoc.backend.controller;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.duoc.backend.dto.RegistroMedicoRequest;
import com.duoc.backend.model.Cita;
import com.duoc.backend.model.Mascota;
import com.duoc.backend.model.RegistroMedico;
import com.duoc.backend.model.Rol;
import com.duoc.backend.model.Veterinario;
import com.duoc.backend.support.IntegrationTestBase;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

class RegistroMedicoControllerTest extends IntegrationTestBase {

    private Mascota mascota;
    private Veterinario veterinario;

    @BeforeEach
    void preparar() {
        mascota = crearMascota(crearPropietario());
        veterinario = crearVeterinario();
    }

    private RegistroMedicoRequest solicitud(LocalDate fecha, Long citaId) {
        return new RegistroMedicoRequest(fecha, "Otitis", "Gotas por 7 dias", mascota.getId(), veterinario.getId(), citaId);
    }

    private RegistroMedico crearRegistro() {
        RegistroMedico registro = new RegistroMedico();
        registro.setFecha(LocalDate.now().minusDays(2));
        registro.setDiagnostico("Control");
        registro.setTratamiento("Ninguno");
        registro.setMascota(mascota);
        registro.setVeterinario(veterinario);
        return registroMedicoRepository.save(registro);
    }

    @Test
    void creaRegistroSinCita() throws Exception {
        enviar(HttpMethod.POST, "/registros-medicos", Rol.VETERINARIO, solicitud(LocalDate.now(), null))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.diagnostico").value("Otitis"))
                .andExpect(jsonPath("$.citaId").doesNotExist());
    }

    @Test
    void creaRegistroConCita() throws Exception {
        Cita cita = crearCita(mascota, veterinario, fechaFutura());
        enviar(HttpMethod.POST, "/registros-medicos", Rol.VETERINARIO, solicitud(LocalDate.now(), cita.getId()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.citaId").value(cita.getId()));
    }

    @Test
    void rechazaFechaFutura() throws Exception {
        enviar(HttpMethod.POST, "/registros-medicos", Rol.ADMIN, solicitud(LocalDate.now().plusDays(2), null))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rechazaReferenciasInexistentes() throws Exception {
        enviar(HttpMethod.POST, "/registros-medicos", Rol.ADMIN, solicitud(LocalDate.now(), 999999L))
                .andExpect(status().isNotFound());
        enviar(HttpMethod.POST, "/registros-medicos", Rol.ADMIN,
                new RegistroMedicoRequest(LocalDate.now(), "A", "B", 999999L, veterinario.getId(), null))
                .andExpect(status().isNotFound());
        enviar(HttpMethod.POST, "/registros-medicos", Rol.ADMIN,
                new RegistroMedicoRequest(LocalDate.now(), "A", "B", mascota.getId(), 999999L, null))
                .andExpect(status().isNotFound());
    }

    @Test
    void listaFiltraYObtiene() throws Exception {
        RegistroMedico registro = crearRegistro();
        Mascota otra = crearMascota(crearPropietario());
        RegistroMedico ajeno = new RegistroMedico();
        ajeno.setFecha(LocalDate.now());
        ajeno.setDiagnostico("X");
        ajeno.setTratamiento("Y");
        ajeno.setMascota(otra);
        ajeno.setVeterinario(veterinario);
        registroMedicoRepository.save(ajeno);
        enviar(HttpMethod.GET, "/registros-medicos", Rol.VETERINARIO, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
        enviar(HttpMethod.GET, "/registros-medicos?mascotaId=" + mascota.getId(), Rol.VETERINARIO, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
        enviar(HttpMethod.GET, "/registros-medicos/" + registro.getId(), Rol.VETERINARIO, null)
                .andExpect(status().isOk());
        enviar(HttpMethod.GET, "/registros-medicos/999999", Rol.VETERINARIO, null)
                .andExpect(status().isNotFound());
    }

    @Test
    void actualizaYEliminaRegistro() throws Exception {
        RegistroMedico registro = crearRegistro();
        enviar(HttpMethod.PUT, "/registros-medicos/" + registro.getId(), Rol.VETERINARIO, solicitud(LocalDate.now(), null))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tratamiento").value("Gotas por 7 dias"));
        enviar(HttpMethod.DELETE, "/registros-medicos/" + registro.getId(), Rol.ADMIN, null)
                .andExpect(status().isNoContent());
    }
}
