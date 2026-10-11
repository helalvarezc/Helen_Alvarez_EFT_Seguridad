package com.duoc.backend.dto;

import com.duoc.backend.model.Propietario;

public record PropietarioResponse(Long id, String nombre, String apellido, String rut, String email, String telefono, String direccion) {

    public static PropietarioResponse desde(Propietario propietario) {
        return new PropietarioResponse(propietario.getId(), propietario.getNombre(), propietario.getApellido(),
                propietario.getRut(), propietario.getEmail(), propietario.getTelefono(), propietario.getDireccion());
    }
}
