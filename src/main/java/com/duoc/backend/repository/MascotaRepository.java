package com.duoc.backend.repository;

import com.duoc.backend.model.Mascota;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MascotaRepository extends JpaRepository<Mascota, Long> {

    List<Mascota> findByPropietarioId(Long propietarioId);

    boolean existsByPropietarioId(Long propietarioId);
}
