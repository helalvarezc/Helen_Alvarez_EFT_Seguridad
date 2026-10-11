package com.duoc.backend.security;

import com.duoc.backend.dto.LoginRequest;
import com.duoc.backend.dto.LoginResponse;
import com.duoc.backend.exception.DemasiadosIntentosException;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String TIPO_TOKEN = "Bearer";

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final LoginAttemptService loginAttemptService;

    public LoginResponse login(LoginRequest request) {
        String clave = request.username().toLowerCase(Locale.ROOT);
        if (loginAttemptService.estaBloqueado(clave)) {
            log.warn("Intento de login sobre una cuenta bloqueada");
            throw new DemasiadosIntentosException("Demasiados intentos fallidos. Intente mas tarde");
        }
        Authentication autenticacion;
        try {
            autenticacion = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        } catch (AuthenticationException e) {
            loginAttemptService.registrarFallo(clave);
            log.warn("Login fallido");
            throw new BadCredentialsException("Credenciales invalidas");
        }
        loginAttemptService.registrarExito(clave);
        List<String> roles = autenticacion.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(autoridad -> autoridad.replaceFirst("^ROLE_", ""))
                .toList();
        log.info("Login exitoso");
        String token = jwtService.generarToken(autenticacion.getName(), roles);
        return new LoginResponse(token, TIPO_TOKEN, jwtService.getExpiracionMs());
    }
}
