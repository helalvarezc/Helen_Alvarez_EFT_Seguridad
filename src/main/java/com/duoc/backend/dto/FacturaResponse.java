package com.duoc.backend.dto;

import com.duoc.backend.model.EstadoFactura;
import com.duoc.backend.model.Factura;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record FacturaResponse(Long id, LocalDateTime fecha, BigDecimal total, EstadoFactura estado,
                              Long propietarioId, Long citaId, List<DetalleFacturaResponse> detalles) {

    public static FacturaResponse desde(Factura factura) {
        Long citaId = factura.getCita() == null ? null : factura.getCita().getId();
        List<DetalleFacturaResponse> detalles = factura.getDetalles().stream().map(DetalleFacturaResponse::desde).toList();
        return new FacturaResponse(factura.getId(), factura.getFecha(), factura.getTotal(), factura.getEstado(),
                factura.getPropietario().getId(), citaId, detalles);
    }
}
