package com.duoc.backend.controller;

import com.duoc.backend.dto.FacturaRequest;
import com.duoc.backend.dto.FacturaResponse;
import com.duoc.backend.service.FacturaService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/facturas")
@RequiredArgsConstructor
public class FacturaController {

    private final FacturaService facturaService;

    @GetMapping
    public List<FacturaResponse> listar() {
        return facturaService.listar();
    }

    @GetMapping("/{id}")
    public FacturaResponse obtener(@PathVariable Long id) {
        return facturaService.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FacturaResponse crear(@Valid @RequestBody FacturaRequest request) {
        return facturaService.crear(request);
    }

    @PatchMapping("/{id}/pagar")
    public FacturaResponse pagar(@PathVariable Long id) {
        return facturaService.pagar(id);
    }

    @PatchMapping("/{id}/anular")
    public FacturaResponse anular(@PathVariable Long id) {
        return facturaService.anular(id);
    }
}
