package com.duoc.backend.controller;

import com.duoc.backend.dto.RegistroMedicoRequest;
import com.duoc.backend.dto.RegistroMedicoResponse;
import com.duoc.backend.service.RegistroMedicoService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/registros-medicos")
@RequiredArgsConstructor
public class RegistroMedicoController {

    private final RegistroMedicoService registroMedicoService;

    @GetMapping
    public List<RegistroMedicoResponse> listar(@RequestParam(required = false) Long mascotaId) {
        return registroMedicoService.listar(mascotaId);
    }

    @GetMapping("/{id}")
    public RegistroMedicoResponse obtener(@PathVariable Long id) {
        return registroMedicoService.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RegistroMedicoResponse crear(@Valid @RequestBody RegistroMedicoRequest request) {
        return registroMedicoService.crear(request);
    }

    @PutMapping("/{id}")
    public RegistroMedicoResponse actualizar(@PathVariable Long id, @Valid @RequestBody RegistroMedicoRequest request) {
        return registroMedicoService.actualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        registroMedicoService.eliminar(id);
    }
}
