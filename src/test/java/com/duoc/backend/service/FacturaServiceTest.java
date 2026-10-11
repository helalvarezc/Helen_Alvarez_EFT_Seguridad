package com.duoc.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.duoc.backend.dto.DetalleFacturaRequest;
import com.duoc.backend.dto.FacturaRequest;
import com.duoc.backend.dto.FacturaResponse;
import com.duoc.backend.exception.ConflictoException;
import com.duoc.backend.exception.RecursoNoEncontradoException;
import com.duoc.backend.model.EstadoFactura;
import com.duoc.backend.model.Factura;
import com.duoc.backend.model.Propietario;
import com.duoc.backend.repository.CitaRepository;
import com.duoc.backend.repository.FacturaRepository;
import com.duoc.backend.repository.PropietarioRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FacturaServiceTest {

    @Mock
    private FacturaRepository facturaRepository;

    @Mock
    private PropietarioRepository propietarioRepository;

    @Mock
    private CitaRepository citaRepository;

    private FacturaService facturaService;
    private Propietario propietario;

    @BeforeEach
    void preparar() {
        Clock reloj = Clock.fixed(Instant.parse("2026-03-01T12:00:00Z"), ZoneOffset.UTC);
        facturaService = new FacturaService(facturaRepository, propietarioRepository, citaRepository, reloj);
        propietario = new Propietario();
        propietario.setId(7L);
    }

    private Factura facturaConEstado(EstadoFactura estado) {
        Factura factura = new Factura();
        factura.setId(3L);
        factura.setFecha(LocalDateTime.of(2026, 3, 1, 12, 0));
        factura.setTotal(new BigDecimal("50.00"));
        factura.setEstado(estado);
        factura.setPropietario(propietario);
        return factura;
    }

    @Test
    void calculaElTotalSumandoLosDetalles() {
        when(propietarioRepository.findById(7L)).thenReturn(Optional.of(propietario));
        when(facturaRepository.save(any(Factura.class))).thenAnswer(invocacion -> invocacion.getArgument(0));
        FacturaRequest solicitud = new FacturaRequest(7L, null, List.of(
                new DetalleFacturaRequest("Consulta", 2, new BigDecimal("10.50")),
                new DetalleFacturaRequest("Vacuna", 1, new BigDecimal("49.99"))));

        FacturaResponse respuesta = facturaService.crear(solicitud);

        assertEquals(new BigDecimal("70.99"), respuesta.total());
        assertEquals(EstadoFactura.PENDIENTE, respuesta.estado());
        assertEquals(2, respuesta.detalles().size());
        assertEquals(LocalDateTime.of(2026, 3, 1, 12, 0), respuesta.fecha());
    }

    @Test
    void rechazaPropietarioInexistente() {
        when(propietarioRepository.findById(7L)).thenReturn(Optional.empty());
        FacturaRequest solicitud = new FacturaRequest(7L, null,
                List.of(new DetalleFacturaRequest("Consulta", 1, new BigDecimal("10.00"))));

        assertThrows(RecursoNoEncontradoException.class, () -> facturaService.crear(solicitud));
    }

    @Test
    void pagaFacturaPendiente() {
        Factura factura = facturaConEstado(EstadoFactura.PENDIENTE);
        when(facturaRepository.findById(3L)).thenReturn(Optional.of(factura));
        when(facturaRepository.save(factura)).thenReturn(factura);

        assertEquals(EstadoFactura.PAGADA, facturaService.pagar(3L).estado());
    }

    @Test
    void noPagaFacturaYaPagada() {
        when(facturaRepository.findById(3L)).thenReturn(Optional.of(facturaConEstado(EstadoFactura.PAGADA)));

        assertThrows(ConflictoException.class, () -> facturaService.pagar(3L));
    }

    @Test
    void anulaFacturaPendienteYNoUnaAnulada() {
        Factura pendiente = facturaConEstado(EstadoFactura.PENDIENTE);
        when(facturaRepository.findById(3L)).thenReturn(Optional.of(pendiente));
        when(facturaRepository.save(pendiente)).thenReturn(pendiente);

        assertEquals(EstadoFactura.ANULADA, facturaService.anular(3L).estado());
        assertThrows(ConflictoException.class, () -> facturaService.anular(3L));
    }
}
