package com.duoc.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VeterinarioRequest(
        @NotBlank @Size(max = 100) String nombre,
        @NotBlank @Size(max = 100) String especialidad,
        @NotBlank @Email @Size(max = 120) String email) {
}
