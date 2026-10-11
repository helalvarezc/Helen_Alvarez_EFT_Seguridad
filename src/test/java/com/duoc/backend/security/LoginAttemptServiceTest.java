package com.duoc.backend.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.duoc.backend.support.RelojMutable;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LoginAttemptServiceTest {

    private RelojMutable reloj;
    private LoginAttemptService servicio;

    @BeforeEach
    void preparar() {
        reloj = new RelojMutable(Instant.parse("2026-01-01T10:00:00Z"));
        servicio = new LoginAttemptService(reloj);
    }

    private void fallar(String clave, int veces) {
        for (int i = 0; i < veces; i++) {
            servicio.registrarFallo(clave);
        }
    }

    @Test
    void noBloqueaAntesDelLimite() {
        fallar("ana", LoginAttemptService.MAX_INTENTOS - 1);
        assertFalse(servicio.estaBloqueado("ana"));
    }

    @Test
    void bloqueaAlAlcanzarElLimite() {
        fallar("ana", LoginAttemptService.MAX_INTENTOS);
        assertTrue(servicio.estaBloqueado("ana"));
    }

    @Test
    void desbloqueaDespuesDelPlazo() {
        fallar("ana", LoginAttemptService.MAX_INTENTOS);
        reloj.avanzar(LoginAttemptService.BLOQUEO.plusSeconds(1));
        assertFalse(servicio.estaBloqueado("ana"));
    }

    @Test
    void exitoReiniciaElContador() {
        fallar("ana", LoginAttemptService.MAX_INTENTOS - 1);
        servicio.registrarExito("ana");
        fallar("ana", 1);
        assertFalse(servicio.estaBloqueado("ana"));
    }

    @Test
    void fallosAntiguosNoSeAcumulan() {
        fallar("ana", LoginAttemptService.MAX_INTENTOS - 1);
        reloj.avanzar(LoginAttemptService.BLOQUEO.plus(Duration.ofMinutes(1)));
        fallar("ana", 1);
        assertFalse(servicio.estaBloqueado("ana"));
    }

    @Test
    void bloqueoEsIndependientePorUsuario() {
        fallar("ana", LoginAttemptService.MAX_INTENTOS);
        assertFalse(servicio.estaBloqueado("luis"));
    }

    @Test
    void usuarioSinRegistrosNoEstaBloqueado() {
        assertFalse(servicio.estaBloqueado("nadie"));
    }
}
