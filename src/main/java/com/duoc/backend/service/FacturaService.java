package com.duoc.backend.service;

import com.duoc.backend.dto.DetalleFacturaRequest;
import com.duoc.backend.dto.FacturaRequest;
import com.duoc.backend.dto.FacturaResponse;
import com.duoc.backend.exception.ConflictoException;
import com.duoc.backend.exception.RecursoNoEncontradoException;
import com.duoc.backend.model.Cita;
import com.duoc.backend.model.DetalleFactura;
import com.duoc.backend.model.EstadoFactura;
import com.duoc.backend.model.Factura;
import com.duoc.backend.model.Propietario;
import com.duoc.backend.repository.CitaRepository;
import com.duoc.backend.repository.FacturaRepository;
import com.duoc.backend.repository.PropietarioRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class FacturaService {

    private final FacturaRepository facturaRepository;
    private final PropietarioRepository propietarioRepository;
    private final CitaRepository citaRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<FacturaResponse> listar() {
        return facturaRepository.findAll().stream().map(FacturaResponse::desde).toList();
    }

    @Transactional(readOnly = true)
    public FacturaResponse obtener(Long id) {
        return FacturaResponse.desde(buscar(id));
    }

    public FacturaResponse crear(FacturaRequest request) {
        Propietario propietario = propietarioRepository.findById(request.propietarioId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Propietario no encontrado"));
        Cita cita = null;
        if (request.citaId() != null) {
            cita = citaRepository.findById(request.citaId())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Cita no encontrada"));
        }
        Factura factura = new Factura();
        factura.setFecha(LocalDateTime.now(clock));
        factura.setEstado(EstadoFactura.PENDIENTE);
        factura.setPropietario(propietario);
        factura.setCita(cita);
        BigDecimal total = BigDecimal.ZERO;
        for (DetalleFacturaRequest solicitud : request.detalles()) {
            DetalleFactura detalle = new DetalleFactura();
            detalle.setDescripcion(solicitud.descripcion());
            detalle.setCantidad(solicitud.cantidad());
            detalle.setPrecioUnitario(solicitud.precioUnitario());
            detalle.setFactura(factura);
            factura.getDetalles().add(detalle);
            total = total.add(solicitud.precioUnitario().multiply(BigDecimal.valueOf(solicitud.cantidad())));
        }
        factura.setTotal(total);
        return FacturaResponse.desde(facturaRepository.save(factura));
    }

    public FacturaResponse pagar(Long id) {
        return cambiarEstado(id, EstadoFactura.PAGADA, "Solo se puede pagar una factura pendiente");
    }

    public FacturaResponse anular(Long id) {
        return cambiarEstado(id, EstadoFactura.ANULADA, "Solo se puede anular una factura pendiente");
    }

    private FacturaResponse cambiarEstado(Long id, EstadoFactura destino, String mensajeConflicto) {
        Factura factura = buscar(id);
        if (factura.getEstado() != EstadoFactura.PENDIENTE) {
            throw new ConflictoException(mensajeConflicto);
        }
        factura.setEstado(destino);
        return FacturaResponse.desde(facturaRepository.save(factura));
    }

    private Factura buscar(Long id) {
        return facturaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Factura no encontrada"));
    }
}
