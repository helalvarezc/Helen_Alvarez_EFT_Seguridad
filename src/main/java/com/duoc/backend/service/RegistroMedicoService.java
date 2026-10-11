package com.duoc.backend.service;

import com.duoc.backend.dto.RegistroMedicoRequest;
import com.duoc.backend.dto.RegistroMedicoResponse;
import com.duoc.backend.exception.RecursoNoEncontradoException;
import com.duoc.backend.model.Cita;
import com.duoc.backend.model.Mascota;
import com.duoc.backend.model.RegistroMedico;
import com.duoc.backend.model.Veterinario;
import com.duoc.backend.repository.CitaRepository;
import com.duoc.backend.repository.MascotaRepository;
import com.duoc.backend.repository.RegistroMedicoRepository;
import com.duoc.backend.repository.VeterinarioRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class RegistroMedicoService {

    private final RegistroMedicoRepository registroMedicoRepository;
    private final MascotaRepository mascotaRepository;
    private final VeterinarioRepository veterinarioRepository;
    private final CitaRepository citaRepository;

    @Transactional(readOnly = true)
    public List<RegistroMedicoResponse> listar(Long mascotaId) {
        List<RegistroMedico> registros = mascotaId == null
                ? registroMedicoRepository.findAll()
                : registroMedicoRepository.findByMascotaIdOrderByFechaDesc(mascotaId);
        return registros.stream().map(RegistroMedicoResponse::desde).toList();
    }

    @Transactional(readOnly = true)
    public RegistroMedicoResponse obtener(Long id) {
        return RegistroMedicoResponse.desde(buscar(id));
    }

    public RegistroMedicoResponse crear(RegistroMedicoRequest request) {
        RegistroMedico registro = new RegistroMedico();
        aplicar(registro, request);
        return RegistroMedicoResponse.desde(registroMedicoRepository.save(registro));
    }

    public RegistroMedicoResponse actualizar(Long id, RegistroMedicoRequest request) {
        RegistroMedico registro = buscar(id);
        aplicar(registro, request);
        return RegistroMedicoResponse.desde(registroMedicoRepository.save(registro));
    }

    public void eliminar(Long id) {
        RegistroMedico registro = buscar(id);
        registroMedicoRepository.delete(registro);
        registroMedicoRepository.flush();
    }

    private RegistroMedico buscar(Long id) {
        return registroMedicoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Registro medico no encontrado"));
    }

    private void aplicar(RegistroMedico registro, RegistroMedicoRequest request) {
        Mascota mascota = mascotaRepository.findById(request.mascotaId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Mascota no encontrada"));
        Veterinario veterinario = veterinarioRepository.findById(request.veterinarioId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Veterinario no encontrado"));
        Cita cita = null;
        if (request.citaId() != null) {
            cita = citaRepository.findById(request.citaId())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Cita no encontrada"));
        }
        registro.setFecha(request.fecha());
        registro.setDiagnostico(request.diagnostico());
        registro.setTratamiento(request.tratamiento());
        registro.setMascota(mascota);
        registro.setVeterinario(veterinario);
        registro.setCita(cita);
    }
}
