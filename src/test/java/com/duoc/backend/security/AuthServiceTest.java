package com.duoc.backend.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.duoc.backend.dto.LoginRequest;
import com.duoc.backend.dto.LoginResponse;
import com.duoc.backend.exception.DemasiadosIntentosException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @Mock
    private LoginAttemptService loginAttemptService;

    @InjectMocks
    private AuthService authService;

    @Test
    void loginExitosoGeneraToken() {
        when(loginAttemptService.estaBloqueado("admin")).thenReturn(false);
        when(authenticationManager.authenticate(any())).thenReturn(
                new UsernamePasswordAuthenticationToken("admin", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
        when(jwtService.generarToken("admin", List.of("ADMIN"))).thenReturn("token-firmado");
        when(jwtService.getExpiracionMs()).thenReturn(3600000L);

        LoginResponse respuesta = authService.login(new LoginRequest("Admin", "Clave#Segura1"));

        assertEquals("token-firmado", respuesta.token());
        assertEquals("Bearer", respuesta.tipo());
        assertEquals(3600000L, respuesta.expiraEnMs());
        verify(loginAttemptService).registrarExito("admin");
    }

    @Test
    void loginFallidoRegistraIntentoYLanzaExcepcion() {
        when(loginAttemptService.estaBloqueado("admin")).thenReturn(false);
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("mal"));

        assertThrows(BadCredentialsException.class, () -> authService.login(new LoginRequest("admin", "x")));

        verify(loginAttemptService).registrarFallo("admin");
        verify(jwtService, never()).generarToken(any(), any());
    }

    @Test
    void cuentaBloqueadaNoIntentaAutenticar() {
        when(loginAttemptService.estaBloqueado("admin")).thenReturn(true);

        assertThrows(DemasiadosIntentosException.class, () -> authService.login(new LoginRequest("admin", "x")));

        verify(authenticationManager, never()).authenticate(any());
    }
}
