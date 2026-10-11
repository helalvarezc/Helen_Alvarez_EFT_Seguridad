package com.duoc.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record FacturaRequest(
        @NotNull Long propietarioId,
        Long citaId,
        @NotEmpty @Size(max = 50) List<@Valid DetalleFacturaRequest> detalles) {
}
