package com.duoc.backend.dto;

import com.duoc.backend.model.Rol;
import com.duoc.backend.model.Usuario;

public record UsuarioResponse(Long id, String username, String email, Rol rol, boolean enabled) {

    public static UsuarioResponse desde(Usuario usuario) {
        return new UsuarioResponse(usuario.getId(), usuario.getUsername(), usuario.getEmail(), usuario.getRol(), usuario.isEnabled());
    }
}
