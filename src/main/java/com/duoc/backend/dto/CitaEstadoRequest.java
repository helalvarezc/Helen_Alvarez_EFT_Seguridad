package com.duoc.backend.dto;

import com.duoc.backend.model.EstadoCita;
import jakarta.validation.constraints.NotNull;

public record CitaEstadoRequest(@NotNull EstadoCita estado) {
}
