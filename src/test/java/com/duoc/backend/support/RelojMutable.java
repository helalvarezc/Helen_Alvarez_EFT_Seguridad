package com.duoc.backend.support;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

public class RelojMutable extends Clock {

    private Instant instante;

    public RelojMutable(Instant instante) {
        this.instante = instante;
    }

    public void avanzar(Duration duracion) {
        instante = instante.plus(duracion);
    }

    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return this;
    }

    @Override
    public Instant instant() {
        return instante;
    }
}
