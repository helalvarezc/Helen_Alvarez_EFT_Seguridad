package com.duoc.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.duoc.backend.dto.UsuarioRequest;
import com.duoc.backend.dto.UsuarioResponse;
import com.duoc.backend.exception.ConflictoException;
import com.duoc.backend.model.Rol;
import com.duoc.backend.model.Usuario;
import com.duoc.backend.repository.UsuarioRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UsuarioService usuarioService;

    private final UsuarioRequest solicitud = new UsuarioRequest("ana.perez", "ana@vet.cl", "Clave#Segura1", Rol.VETERINARIO);

    @Test
    void guardaLaPasswordCifrada() {
        when(usuarioRepository.existsByUsername("ana.perez")).thenReturn(false);
        when(usuarioRepository.existsByEmail("ana@vet.cl")).thenReturn(false);
        when(passwordEncoder.encode("Clave#Segura1")).thenReturn("hash-bcrypt");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        UsuarioResponse respuesta = usuarioService.crear(solicitud);

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        assertEquals("hash-bcrypt", captor.getValue().getPassword());
        assertNotEquals("Clave#Segura1", captor.getValue().getPassword());
        assertEquals(Rol.VETERINARIO, respuesta.rol());
    }

    @Test
    void rechazaNombreDeUsuarioDuplicado() {
        when(usuarioRepository.existsByUsername("ana.perez")).thenReturn(true);

        assertThrows(ConflictoException.class, () -> usuarioService.crear(solicitud));

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void rechazaCorreoDuplicado() {
        when(usuarioRepository.existsByUsername("ana.perez")).thenReturn(false);
        when(usuarioRepository.existsByEmail("ana@vet.cl")).thenReturn(true);

        assertThrows(ConflictoException.class, () -> usuarioService.crear(solicitud));
    }

    @Test
    void listaUsuarios() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setUsername("ana.perez");
        usuario.setRol(Rol.ADMIN);
        when(usuarioRepository.findAll()).thenReturn(List.of(usuario));

        assertEquals(1, usuarioService.listar().size());
    }
}
