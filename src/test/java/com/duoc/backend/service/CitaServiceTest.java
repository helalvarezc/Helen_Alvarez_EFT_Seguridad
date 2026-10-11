package com.duoc.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.duoc.backend.dto.CitaRequest;
import com.duoc.backend.dto.CitaResponse;
import com.duoc.backend.exception.ConflictoException;
import com.duoc.backend.exception.RecursoNoEncontradoException;
import com.duoc.backend.model.Cita;
import com.duoc.backend.model.EstadoCita;
import com.duoc.backend.model.Mascota;
import com.duoc.backend.model.Veterinario;
import com.duoc.backend.repository.CitaRepository;
import com.duoc.backend.repository.MascotaRepository;
import com.duoc.backend.repository.VeterinarioRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CitaServiceTest {

    private static final LocalDateTime HORARIO = LocalDateTime.of(2030, 5, 10, 9, 0);

    @Mock
    private CitaRepository citaRepository;

    @Mock
    private MascotaRepository mascotaRepository;

    @Mock
    private VeterinarioRepository veterinarioRepository;

    @InjectMocks
    private CitaService citaService;

    private Mascota mascota;
    private Veterinario veterinario;
    private CitaRequest solicitud;

    @BeforeEach
    void preparar() {
        mascota = new Mascota();
        mascota.setId(1L);
        mascota.setNombre("Firulais");
        veterinario = new Veterinario();
        veterinario.setId(2L);
        veterinario.setNombre("Dr. Soto");
        solicitud = new CitaRequest(HORARIO, "Control", 1L, 2L);
    }

    private Cita citaConEstado(EstadoCita estado) {
        Cita cita = new Cita();
        cita.setId(9L);
        cita.setEstado(estado);
        cita.setFechaHora(HORARIO);
        cita.setMotivo("Control");
        cita.setMascota(mascota);
        cita.setVeterinario(veterinario);
        return cita;
    }

    @Test
    void creaCitaProgramada() {
        when(citaRepository.existsByVeterinarioIdAndFechaHoraAndEstadoNot(2L, HORARIO, EstadoCita.CANCELADA)).thenReturn(false);
        when(mascotaRepository.findById(1L)).thenReturn(Optional.of(mascota));
        when(veterinarioRepository.findById(2L)).thenReturn(Optional.of(veterinario));
        when(citaRepository.save(any(Cita.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        CitaResponse respuesta = citaService.crear(solicitud);

        assertEquals(EstadoCita.PROGRAMADA, respuesta.estado());
        assertEquals("Firulais", respuesta.mascotaNombre());
        assertEquals("Dr. Soto", respuesta.veterinarioNombre());
    }

    @Test
    void rechazaHorarioOcupado() {
        when(citaRepository.existsByVeterinarioIdAndFechaHoraAndEstadoNot(2L, HORARIO, EstadoCita.CANCELADA)).thenReturn(true);

        assertThrows(ConflictoException.class, () -> citaService.crear(solicitud));

        verify(citaRepository, never()).save(any());
    }

    @Test
    void rechazaMascotaInexistente() {
        when(citaRepository.existsByVeterinarioIdAndFechaHoraAndEstadoNot(2L, HORARIO, EstadoCita.CANCELADA)).thenReturn(false);
        when(mascotaRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> citaService.crear(solicitud));
    }

    @Test
    void cambiaEstadoDeCitaProgramada() {
        Cita cita = citaConEstado(EstadoCita.PROGRAMADA);
        when(citaRepository.findById(9L)).thenReturn(Optional.of(cita));
        when(citaRepository.save(cita)).thenReturn(cita);

        CitaResponse respuesta = citaService.cambiarEstado(9L, EstadoCita.COMPLETADA);

        assertEquals(EstadoCita.COMPLETADA, respuesta.estado());
    }

    @Test
    void noCambiaEstadoDeCitaFinalizada() {
        when(citaRepository.findById(9L)).thenReturn(Optional.of(citaConEstado(EstadoCita.COMPLETADA)));

        assertThrows(ConflictoException.class, () -> citaService.cambiarEstado(9L, EstadoCita.CANCELADA));
    }

    @Test
    void noPermiteEstadoDestinoProgramada() {
        when(citaRepository.findById(9L)).thenReturn(Optional.of(citaConEstado(EstadoCita.PROGRAMADA)));

        assertThrows(ConflictoException.class, () -> citaService.cambiarEstado(9L, EstadoCita.PROGRAMADA));
    }

    @Test
    void actualizaExcluyendoLaPropiaCita() {
        Cita cita = citaConEstado(EstadoCita.PROGRAMADA);
        when(citaRepository.findById(9L)).thenReturn(Optional.of(cita));
        when(citaRepository.existsByVeterinarioIdAndFechaHoraAndEstadoNotAndIdNot(eq(2L), eq(HORARIO), eq(EstadoCita.CANCELADA), eq(9L)))
                .thenReturn(false);
        when(mascotaRepository.findById(1L)).thenReturn(Optional.of(mascota));
        when(veterinarioRepository.findById(2L)).thenReturn(Optional.of(veterinario));
        when(citaRepository.save(cita)).thenReturn(cita);

        assertEquals(9L, citaService.actualizar(9L, solicitud).id());
    }
}
