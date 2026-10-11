package com.duoc.backend.repository;

import com.duoc.backend.model.Factura;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FacturaRepository extends JpaRepository<Factura, Long> {

    boolean existsByPropietarioId(Long propietarioId);
}
