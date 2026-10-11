package com.duoc.backend.service;

import com.duoc.backend.dto.VeterinarioRequest;
import com.duoc.backend.dto.VeterinarioResponse;
import com.duoc.backend.exception.ConflictoException;
import com.duoc.backend.exception.RecursoNoEncontradoException;
import com.duoc.backend.model.Veterinario;
import com.duoc.backend.repository.CitaRepository;
import com.duoc.backend.repository.RegistroMedicoRepository;
import com.duoc.backend.repository.VeterinarioRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class VeterinarioService {

    private final VeterinarioRepository veterinarioRepository;
    private final CitaRepository citaRepository;
    private final RegistroMedicoRepository registroMedicoRepository;

    @Transactional(readOnly = true)
    public List<VeterinarioResponse> listar() {
        return veterinarioRepository.findAll().stream().map(VeterinarioResponse::desde).toList();
    }

    @Transactional(readOnly = true)
    public VeterinarioResponse obtener(Long id) {
        return VeterinarioResponse.desde(buscar(id));
    }

    public VeterinarioResponse crear(VeterinarioRequest request) {
        if (veterinarioRepository.existsByEmail(request.email())) {
            throw new ConflictoException("Ya existe un veterinario con ese correo");
        }
        Veterinario veterinario = new Veterinario();
        aplicar(veterinario, request);
        return VeterinarioResponse.desde(veterinarioRepository.save(veterinario));
    }

    public VeterinarioResponse actualizar(Long id, VeterinarioRequest request) {
        Veterinario veterinario = buscar(id);
        if (veterinarioRepository.existsByEmailAndIdNot(request.email(), id)) {
            throw new ConflictoException("Ya existe un veterinario con ese correo");
        }
        aplicar(veterinario, request);
        return VeterinarioResponse.desde(veterinarioRepository.save(veterinario));
    }

    public void eliminar(Long id) {
        Veterinario veterinario = buscar(id);
        if (citaRepository.existsByVeterinarioId(id) || registroMedicoRepository.existsByVeterinarioId(id)) {
            throw new ConflictoException("El veterinario tiene citas o registros medicos asociados");
        }
        veterinarioRepository.delete(veterinario);
        veterinarioRepository.flush();
    }

    private Veterinario buscar(Long id) {
        return veterinarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Veterinario no encontrado"));
    }

    private void aplicar(Veterinario veterinario, VeterinarioRequest request) {
        veterinario.setNombre(request.nombre());
        veterinario.setEspecialidad(request.especialidad());
        veterinario.setEmail(request.email());
    }
}
