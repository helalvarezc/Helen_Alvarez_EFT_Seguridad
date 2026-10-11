package com.duoc.backend.dto;

import com.duoc.backend.model.RegistroMedico;
import java.time.LocalDate;

public record RegistroMedicoResponse(Long id, LocalDate fecha, String diagnostico, String tratamiento,
                                     Long mascotaId, Long veterinarioId, Long citaId) {

    public static RegistroMedicoResponse desde(RegistroMedico registro) {
        Long citaId = registro.getCita() == null ? null : registro.getCita().getId();
        return new RegistroMedicoResponse(registro.getId(), registro.getFecha(), registro.getDiagnostico(),
                registro.getTratamiento(), registro.getMascota().getId(), registro.getVeterinario().getId(), citaId);
    }
}
