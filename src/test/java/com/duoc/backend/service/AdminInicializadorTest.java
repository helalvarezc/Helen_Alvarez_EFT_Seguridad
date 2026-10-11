package com.duoc.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.duoc.backend.config.AdminInicializador;
import com.duoc.backend.model.Rol;
import com.duoc.backend.model.Usuario;
import com.duoc.backend.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;


@ExtendWith(MockitoExtension.class)
class AdminInicializadorTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AdminInicializador inicializador;

    @Test
    void noCreaAdminSiNoHayPasswordConfigurada() {
        ReflectionTestUtils.setField(inicializador, "adminPassword", "");

        inicializador.run(null);

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void noCreaAdminSiYaExiste() {
        ReflectionTestUtils.setField(inicializador, "adminPassword", "Clave#Segura1");
        when(usuarioRepository.existsByUsername("admin")).thenReturn(true);

        inicializador.run(null);

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void creaAdminConPasswordCifrada() {
        ReflectionTestUtils.setField(inicializador, "adminPassword", "Clave#Segura1");
        when(usuarioRepository.existsByUsername("admin")).thenReturn(false);
        when(passwordEncoder.encode("Clave#Segura1")).thenReturn("hash");

        inicializador.run(null);

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        assertEquals("hash", captor.getValue().getPassword());
        assertEquals(Rol.ADMIN, captor.getValue().getRol());
    }
}
