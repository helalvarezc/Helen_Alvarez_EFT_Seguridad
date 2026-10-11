package com.duoc.backend.controller;

import com.duoc.backend.dto.PropietarioRequest;
import com.duoc.backend.dto.PropietarioResponse;
import com.duoc.backend.service.PropietarioService;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/propietarios")
@RequiredArgsConstructor
public class PropietarioController {

    private final PropietarioService propietarioService;

    @GetMapping
    public List<PropietarioResponse> listar() {
        return propietarioService.listar();
    }

    @GetMapping("/{id}")
    public PropietarioResponse obtener(@PathVariable Long id) {
        return propietarioService.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PropietarioResponse crear(@Valid @RequestBody PropietarioRequest request) {
        return propietarioService.crear(request);
    }

    @PutMapping("/{id}")
    public PropietarioResponse actualizar(@PathVariable Long id, @Valid @RequestBody PropietarioRequest request) {
        return propietarioService.actualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        propietarioService.eliminar(id);
    }
}
