package com.duoc.backend.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.duoc.backend.model.Rol;
import com.duoc.backend.model.Usuario;
import com.duoc.backend.repository.UsuarioRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private CustomUserDetailsService servicio;

    @Test
    void cargaUsuarioConSuRol() {
        Usuario usuario = new Usuario();
        usuario.setUsername("ana");
        usuario.setPassword("hash");
        usuario.setRol(Rol.VETERINARIO);
        usuario.setEnabled(true);
        when(usuarioRepository.findByUsername("ana")).thenReturn(Optional.of(usuario));

        UserDetails detalle = servicio.loadUserByUsername("ana");

        assertEquals("ana", detalle.getUsername());
        assertEquals("hash", detalle.getPassword());
        assertTrue(detalle.isEnabled());
        assertTrue(detalle.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_VETERINARIO")));
    }

    @Test
    void marcaUsuarioDeshabilitado() {
        Usuario usuario = new Usuario();
        usuario.setUsername("luis");
        usuario.setPassword("hash");
        usuario.setRol(Rol.RECEPCIONISTA);
        usuario.setEnabled(false);
        when(usuarioRepository.findByUsername("luis")).thenReturn(Optional.of(usuario));

        assertFalse(servicio.loadUserByUsername("luis").isEnabled());
    }

    @Test
    void lanzaExcepcionSiNoExiste() {
        when(usuarioRepository.findByUsername("nadie")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> servicio.loadUserByUsername("nadie"));
    }
}
