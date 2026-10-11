package com.duoc.backend.service;

import com.duoc.backend.dto.CitaRequest;
import com.duoc.backend.dto.CitaResponse;
import com.duoc.backend.exception.ConflictoException;
import com.duoc.backend.exception.RecursoNoEncontradoException;
import com.duoc.backend.model.Cita;
import com.duoc.backend.model.EstadoCita;
import com.duoc.backend.model.Mascota;
import com.duoc.backend.model.Veterinario;
import com.duoc.backend.repository.CitaRepository;
import com.duoc.backend.repository.MascotaRepository;
import com.duoc.backend.repository.VeterinarioRepository;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CitaService {

    private static final String MENSAJE_HORARIO = "El veterinario ya tiene una cita en ese horario";

    private final CitaRepository citaRepository;
    private final MascotaRepository mascotaRepository;
    private final VeterinarioRepository veterinarioRepository;

    @Transactional(readOnly = true)
    public List<CitaResponse> listar(LocalDate fecha) {
        List<Cita> citas = fecha == null
                ? citaRepository.findAll()
                : citaRepository.findByFechaHoraBetweenOrderByFechaHoraAsc(fecha.atStartOfDay(), fecha.plusDays(1).atStartOfDay());
        return citas.stream().map(CitaResponse::desde).toList();
    }

    @Transactional(readOnly = true)
    public CitaResponse obtener(Long id) {
        return CitaResponse.desde(buscar(id));
    }

    public CitaResponse crear(CitaRequest request) {
        if (citaRepository.existsByVeterinarioIdAndFechaHoraAndEstadoNot(
                request.veterinarioId(), request.fechaHora(), EstadoCita.CANCELADA)) {
            throw new ConflictoException(MENSAJE_HORARIO);
        }
        Cita cita = new Cita();
        cita.setEstado(EstadoCita.PROGRAMADA);
        aplicar(cita, request);
        return CitaResponse.desde(citaRepository.save(cita));
    }

    public CitaResponse actualizar(Long id, CitaRequest request) {
        Cita cita = buscar(id);
        if (cita.getEstado() != EstadoCita.PROGRAMADA) {
            throw new ConflictoException("Solo se puede modificar una cita programada");
        }
        if (citaRepository.existsByVeterinarioIdAndFechaHoraAndEstadoNotAndIdNot(
                request.veterinarioId(), request.fechaHora(), EstadoCita.CANCELADA, id)) {
            throw new ConflictoException(MENSAJE_HORARIO);
        }
        aplicar(cita, request);
        return CitaResponse.desde(citaRepository.save(cita));
    }

    public CitaResponse cambiarEstado(Long id, EstadoCita nuevoEstado) {
        Cita cita = buscar(id);
        if (cita.getEstado() != EstadoCita.PROGRAMADA) {
            throw new ConflictoException("Solo se puede cambiar el estado de una cita programada");
        }
        if (nuevoEstado == EstadoCita.PROGRAMADA) {
            throw new ConflictoException("El estado de destino no es valido");
        }
        cita.setEstado(nuevoEstado);
        return CitaResponse.desde(citaRepository.save(cita));
    }

    public void eliminar(Long id) {
        Cita cita = buscar(id);
        citaRepository.delete(cita);
        citaRepository.flush();
    }

    private Cita buscar(Long id) {
        return citaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cita no encontrada"));
    }

    private void aplicar(Cita cita, CitaRequest request) {
        Mascota mascota = mascotaRepository.findById(request.mascotaId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Mascota no encontrada"));
        Veterinario veterinario = veterinarioRepository.findById(request.veterinarioId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Veterinario no encontrado"));
        cita.setFechaHora(request.fechaHora());
        cita.setMotivo(request.motivo());
        cita.setMascota(mascota);
        cita.setVeterinario(veterinario);
    }
}
