package com.duoc.backend.dto;

import com.duoc.backend.model.DetalleFactura;
import java.math.BigDecimal;

public record DetalleFacturaResponse(String descripcion, Integer cantidad, BigDecimal precioUnitario, BigDecimal subtotal) {

    public static DetalleFacturaResponse desde(DetalleFactura detalle) {
        BigDecimal subtotal = detalle.getPrecioUnitario().multiply(BigDecimal.valueOf(detalle.getCantidad()));
        return new DetalleFacturaResponse(detalle.getDescripcion(), detalle.getCantidad(), detalle.getPrecioUnitario(), subtotal);
    }
}
