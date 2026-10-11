package com.duoc.backend.dto;

import com.duoc.backend.model.Cita;
import com.duoc.backend.model.EstadoCita;
import java.time.LocalDateTime;

public record CitaResponse(Long id, LocalDateTime fechaHora, String motivo, EstadoCita estado,
                           Long mascotaId, String mascotaNombre, Long veterinarioId, String veterinarioNombre) {

    public static CitaResponse desde(Cita cita) {
        return new CitaResponse(cita.getId(), cita.getFechaHora(), cita.getMotivo(), cita.getEstado(),
                cita.getMascota().getId(), cita.getMascota().getNombre(),
                cita.getVeterinario().getId(), cita.getVeterinario().getNombre());
    }
}
