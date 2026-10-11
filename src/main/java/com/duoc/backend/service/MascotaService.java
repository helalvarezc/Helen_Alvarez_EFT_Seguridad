package com.duoc.backend.service;

import com.duoc.backend.dto.MascotaRequest;
import com.duoc.backend.dto.MascotaResponse;
import com.duoc.backend.exception.ConflictoException;
import com.duoc.backend.exception.RecursoNoEncontradoException;
import com.duoc.backend.model.Mascota;
import com.duoc.backend.model.Propietario;
import com.duoc.backend.repository.CitaRepository;
import com.duoc.backend.repository.MascotaRepository;
import com.duoc.backend.repository.PropietarioRepository;
import com.duoc.backend.repository.RegistroMedicoRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class MascotaService {

    private final MascotaRepository mascotaRepository;
    private final PropietarioRepository propietarioRepository;
    private final CitaRepository citaRepository;
    private final RegistroMedicoRepository registroMedicoRepository;

    @Transactional(readOnly = true)
    public List<MascotaResponse> listar(Long propietarioId) {
        List<Mascota> mascotas = propietarioId == null
                ? mascotaRepository.findAll()
                : mascotaRepository.findByPropietarioId(propietarioId);
        return mascotas.stream().map(MascotaResponse::desde).toList();
    }

    @Transactional(readOnly = true)
    public MascotaResponse obtener(Long id) {
        return MascotaResponse.desde(buscar(id));
    }

    public MascotaResponse crear(MascotaRequest request) {
        Mascota mascota = new Mascota();
        aplicar(mascota, request);
        return MascotaResponse.desde(mascotaRepository.save(mascota));
    }

    public MascotaResponse actualizar(Long id, MascotaRequest request) {
        Mascota mascota = buscar(id);
        aplicar(mascota, request);
        return MascotaResponse.desde(mascotaRepository.save(mascota));
    }

    public void eliminar(Long id) {
        Mascota mascota = buscar(id);
        if (citaRepository.existsByMascotaId(id) || registroMedicoRepository.existsByMascotaId(id)) {
            throw new ConflictoException("La mascota tiene citas o registros medicos asociados");
        }
        mascotaRepository.delete(mascota);
        mascotaRepository.flush();
    }

    private Mascota buscar(Long id) {
        return mascotaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Mascota no encontrada"));
    }

    private void aplicar(Mascota mascota, MascotaRequest request) {
        Propietario propietario = propietarioRepository.findById(request.propietarioId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Propietario no encontrado"));
        mascota.setNombre(request.nombre());
        mascota.setEspecie(request.especie());
        mascota.setRaza(request.raza());
        mascota.setEdad(request.edad());
        mascota.setPropietario(propietario);
    }
}
