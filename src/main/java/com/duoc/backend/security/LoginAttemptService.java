package com.duoc.backend.security;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class LoginAttemptService {

    static final int MAX_INTENTOS = 5;
    static final Duration BLOQUEO = Duration.ofMinutes(15);
    private static final int LIMITE_REGISTROS = 10_000;

    private final Clock clock;
    private final Map<String, Intento> intentos = new ConcurrentHashMap<>();

    public LoginAttemptService(Clock clock) {
        this.clock = clock;
    }

    public boolean estaBloqueado(String clave) {
        Intento intento = intentos.get(clave);
        if (intento == null || intento.bloqueadoHasta() == null) {
            return false;
        }
        if (clock.instant().isAfter(intento.bloqueadoHasta())) {
            intentos.remove(clave);
            return false;
        }
        return true;
    }

    public void registrarFallo(String clave) {
        if (intentos.size() >= LIMITE_REGISTROS) {
            limpiarExpirados();
        }
        Instant ahora = clock.instant();
        intentos.compute(clave, (k, actual) -> {
            boolean vigente = actual != null && actual.ultimo().plus(BLOQUEO).isAfter(ahora);
            int conteo = vigente ? actual.conteo() + 1 : 1;
            Instant bloqueadoHasta = conteo >= MAX_INTENTOS ? ahora.plus(BLOQUEO) : null;
            return new Intento(conteo, bloqueadoHasta, ahora);
        });
    }

    public void registrarExito(String clave) {
        intentos.remove(clave);
    }

    private void limpiarExpirados() {
        Instant ahora = clock.instant();
        intentos.entrySet().removeIf(entrada -> entrada.getValue().ultimo().plus(BLOQUEO).isBefore(ahora));
    }

    private record Intento(int conteo, Instant bloqueadoHasta, Instant ultimo) {
    }
}
