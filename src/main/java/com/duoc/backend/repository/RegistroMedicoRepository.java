package com.duoc.backend.repository;

import com.duoc.backend.model.RegistroMedico;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RegistroMedicoRepository extends JpaRepository<RegistroMedico, Long> {

    List<RegistroMedico> findByMascotaIdOrderByFechaDesc(Long mascotaId);

    boolean existsByMascotaId(Long mascotaId);

    boolean existsByVeterinarioId(Long veterinarioId);
}
