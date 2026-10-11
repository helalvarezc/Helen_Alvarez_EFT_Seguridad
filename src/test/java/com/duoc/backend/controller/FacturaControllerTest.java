package com.duoc.backend.controller;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.duoc.backend.dto.DetalleFacturaRequest;
import com.duoc.backend.dto.FacturaRequest;
import com.duoc.backend.model.Cita;
import com.duoc.backend.model.EstadoFactura;
import com.duoc.backend.model.Factura;
import com.duoc.backend.model.Propietario;
import com.duoc.backend.model.Rol;
import com.duoc.backend.support.IntegrationTestBase;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

class FacturaControllerTest extends IntegrationTestBase {

    private Propietario propietario;

    @BeforeEach
    void preparar() {
        propietario = crearPropietario();
    }

    private FacturaRequest solicitud(Long propietarioId, Long citaId) {
        return new FacturaRequest(propietarioId, citaId, List.of(
                new DetalleFacturaRequest("Consulta", 2, new BigDecimal("10.50")),
                new DetalleFacturaRequest("Vacuna", 1, new BigDecimal("49.99"))));
    }

    private Factura crearFactura() {
        Factura factura = new Factura();
        factura.setFecha(LocalDateTime.now());
        factura.setTotal(new BigDecimal("100.00"));
        factura.setEstado(EstadoFactura.PENDIENTE);
        factura.setPropietario(propietario);
        return facturaRepository.save(factura);
    }

    @Test
    void creaFacturaConTotalCalculadoEnServidor() throws Exception {
        enviar(HttpMethod.POST, "/facturas", Rol.RECEPCIONISTA, solicitud(propietario.getId(), null))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.total").value(70.99))
                .andExpect(jsonPath("$.estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.detalles.length()").value(2))
                .andExpect(jsonPath("$.detalles[0].subtotal").value(21.0));
    }

    @Test
    void creaFacturaAsociadaACita() throws Exception {
        Cita cita = crearCita(crearMascota(propietario), crearVeterinario(), fechaFutura());
        enviar(HttpMethod.POST, "/facturas", Rol.ADMIN, solicitud(propietario.getId(), cita.getId()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.citaId").value(cita.getId()));
    }

    @Test
    void rechazaFacturaSinDetalles() throws Exception {
        enviar(HttpMethod.POST, "/facturas", Rol.ADMIN, new FacturaRequest(propietario.getId(), null, List.of()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rechazaDetalleConPrecioInvalido() throws Exception {
        FacturaRequest solicitud = new FacturaRequest(propietario.getId(), null,
                List.of(new DetalleFacturaRequest("Consulta", 1, new BigDecimal("0.00"))));
        enviar(HttpMethod.POST, "/facturas", Rol.ADMIN, solicitud).andExpect(status().isBadRequest());
    }

    @Test
    void rechazaPropietarioOCitaInexistente() throws Exception {
        enviar(HttpMethod.POST, "/facturas", Rol.ADMIN, solicitud(999999L, null)).andExpect(status().isNotFound());
        enviar(HttpMethod.POST, "/facturas", Rol.ADMIN, solicitud(propietario.getId(), 999999L))
                .andExpect(status().isNotFound());
    }

    @Test
    void listaYObtieneFacturas() throws Exception {
        Factura factura = crearFactura();
        enviar(HttpMethod.GET, "/facturas", Rol.RECEPCIONISTA, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
        enviar(HttpMethod.GET, "/facturas/" + factura.getId(), Rol.RECEPCIONISTA, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(100.0));
        enviar(HttpMethod.GET, "/facturas/999999", Rol.RECEPCIONISTA, null)
                .andExpect(status().isNotFound());
    }

    @Test
    void pagaFacturaUnaSolaVez() throws Exception {
        Factura factura = crearFactura();
        enviar(HttpMethod.PATCH, "/facturas/" + factura.getId() + "/pagar", Rol.RECEPCIONISTA, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("PAGADA"));
        enviar(HttpMethod.PATCH, "/facturas/" + factura.getId() + "/pagar", Rol.RECEPCIONISTA, null)
                .andExpect(status().isConflict());
        enviar(HttpMethod.PATCH, "/facturas/" + factura.getId() + "/anular", Rol.ADMIN, null)
                .andExpect(status().isConflict());
    }

    @Test
    void anulaFacturaPendiente() throws Exception {
        Factura factura = crearFactura();
        enviar(HttpMethod.PATCH, "/facturas/" + factura.getId() + "/anular", Rol.ADMIN, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ANULADA"));
    }
}
