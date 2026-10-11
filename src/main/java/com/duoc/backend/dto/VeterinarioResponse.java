package com.duoc.backend.dto;

import com.duoc.backend.model.Veterinario;

public record VeterinarioResponse(Long id, String nombre, String especialidad, String email) {

    public static VeterinarioResponse desde(Veterinario veterinario) {
        return new VeterinarioResponse(veterinario.getId(), veterinario.getNombre(), veterinario.getEspecialidad(),
                veterinario.getEmail());
    }
}
