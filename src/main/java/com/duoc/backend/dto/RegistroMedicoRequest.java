package com.duoc.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record RegistroMedicoRequest(
        @NotNull @PastOrPresent LocalDate fecha,
        @NotBlank @Size(max = 1000) String diagnostico,
        @NotBlank @Size(max = 1000) String tratamiento,
        @NotNull Long mascotaId,
        @NotNull Long veterinarioId,
        Long citaId) {
}
