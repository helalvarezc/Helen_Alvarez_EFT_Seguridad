package com.duoc.backend.service;

import com.duoc.backend.dto.UsuarioRequest;
import com.duoc.backend.dto.UsuarioResponse;
import com.duoc.backend.exception.ConflictoException;
import com.duoc.backend.model.Usuario;
import com.duoc.backend.repository.UsuarioRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<UsuarioResponse> listar() {
        return usuarioRepository.findAll().stream().map(UsuarioResponse::desde).toList();
    }

    public UsuarioResponse crear(UsuarioRequest request) {
        if (usuarioRepository.existsByUsername(request.username())) {
            throw new ConflictoException("El nombre de usuario ya existe");
        }
        if (usuarioRepository.existsByEmail(request.email())) {
            throw new ConflictoException("El correo ya esta registrado");
        }
        Usuario usuario = new Usuario();
        usuario.setUsername(request.username());
        usuario.setEmail(request.email());
        usuario.setPassword(passwordEncoder.encode(request.password()));
        usuario.setRol(request.rol());
        usuario.setEnabled(true);
        return UsuarioResponse.desde(usuarioRepository.save(usuario));
    }
}
