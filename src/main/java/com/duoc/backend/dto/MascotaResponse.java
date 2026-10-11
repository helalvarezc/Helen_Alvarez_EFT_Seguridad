package com.duoc.backend.dto;

import com.duoc.backend.model.Mascota;

public record MascotaResponse(Long id, String nombre, String especie, String raza, Integer edad, Long propietarioId) {

    public static MascotaResponse desde(Mascota mascota) {
        return new MascotaResponse(mascota.getId(), mascota.getNombre(), mascota.getEspecie(), mascota.getRaza(),
                mascota.getEdad(), mascota.getPropietario().getId());
    }
}
