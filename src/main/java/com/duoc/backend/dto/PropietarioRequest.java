package com.duoc.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PropietarioRequest(
        @NotBlank @Size(max = 100) String nombre,
        @NotBlank @Size(max = 100) String apellido,
        @NotBlank @Pattern(regexp = "^\\d{7,8}-[\\dkK]$") String rut,
        @NotBlank @Email @Size(max = 120) String email,
        @NotBlank @Pattern(regexp = "^\\+?\\d{8,15}$") String telefono,
        @NotBlank @Size(max = 200) String direccion) {
}
