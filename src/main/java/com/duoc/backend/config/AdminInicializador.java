package com.duoc.backend.config;

import com.duoc.backend.model.Rol;
import com.duoc.backend.model.Usuario;
import com.duoc.backend.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminInicializador implements ApplicationRunner {

    private static final String USUARIO_ADMIN = "admin";

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.bootstrap.admin-password:}")
    private String adminPassword;

    @Override
    public void run(ApplicationArguments args) {
        if (adminPassword == null || adminPassword.isBlank() || usuarioRepository.existsByUsername(USUARIO_ADMIN)) {
            return;
        }
        Usuario admin = new Usuario();
        admin.setUsername(USUARIO_ADMIN);
        admin.setEmail("admin@veterinaria.local");
        admin.setPassword(passwordEncoder.encode(adminPassword));
        admin.setRol(Rol.ADMIN);
        admin.setEnabled(true);
        usuarioRepository.save(admin);
        log.info("Usuario administrador inicial creado");
    }
}
