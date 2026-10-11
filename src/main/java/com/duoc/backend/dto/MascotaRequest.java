package com.duoc.backend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MascotaRequest(
        @NotBlank @Size(max = 80) String nombre,
        @NotBlank @Size(max = 40) String especie,
        @NotBlank @Size(max = 80) String raza,
        @NotNull @Min(0) @Max(60) Integer edad,
        @NotNull Long propietarioId) {
}
