package com.duoc.backend.repository;

import com.duoc.backend.model.Cita;
import com.duoc.backend.model.EstadoCita;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CitaRepository extends JpaRepository<Cita, Long> {

    boolean existsByVeterinarioIdAndFechaHoraAndEstadoNot(Long veterinarioId, LocalDateTime fechaHora, EstadoCita estado);

    boolean existsByVeterinarioIdAndFechaHoraAndEstadoNotAndIdNot(Long veterinarioId, LocalDateTime fechaHora, EstadoCita estado, Long id);

    boolean existsByMascotaId(Long mascotaId);

    boolean existsByVeterinarioId(Long veterinarioId);

    List<Cita> findByFechaHoraBetweenOrderByFechaHoraAsc(LocalDateTime desde, LocalDateTime hasta);
}
