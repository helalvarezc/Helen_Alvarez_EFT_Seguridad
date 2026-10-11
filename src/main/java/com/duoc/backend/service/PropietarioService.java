package com.duoc.backend.service;

import com.duoc.backend.dto.PropietarioRequest;
import com.duoc.backend.dto.PropietarioResponse;
import com.duoc.backend.exception.ConflictoException;
import com.duoc.backend.exception.RecursoNoEncontradoException;
import com.duoc.backend.model.Propietario;
import com.duoc.backend.repository.FacturaRepository;
import com.duoc.backend.repository.MascotaRepository;
import com.duoc.backend.repository.PropietarioRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class PropietarioService {

    private final PropietarioRepository propietarioRepository;
    private final MascotaRepository mascotaRepository;
    private final FacturaRepository facturaRepository;

    @Transactional(readOnly = true)
    public List<PropietarioResponse> listar() {
        return propietarioRepository.findAll().stream().map(PropietarioResponse::desde).toList();
    }

    @Transactional(readOnly = true)
    public PropietarioResponse obtener(Long id) {
        return PropietarioResponse.desde(buscar(id));
    }

    public PropietarioResponse crear(PropietarioRequest request) {
        if (propietarioRepository.existsByRut(request.rut())) {
            throw new ConflictoException("Ya existe un propietario con ese RUT");
        }
        Propietario propietario = new Propietario();
        aplicar(propietario, request);
        return PropietarioResponse.desde(propietarioRepository.save(propietario));
    }

    public PropietarioResponse actualizar(Long id, PropietarioRequest request) {
        Propietario propietario = buscar(id);
        if (propietarioRepository.existsByRutAndIdNot(request.rut(), id)) {
            throw new ConflictoException("Ya existe un propietario con ese RUT");
        }
        aplicar(propietario, request);
        return PropietarioResponse.desde(propietarioRepository.save(propietario));
    }

    public void eliminar(Long id) {
        Propietario propietario = buscar(id);
        if (mascotaRepository.existsByPropietarioId(id) || facturaRepository.existsByPropietarioId(id)) {
            throw new ConflictoException("El propietario tiene mascotas o facturas asociadas");
        }
        propietarioRepository.delete(propietario);
        propietarioRepository.flush();
    }

    private Propietario buscar(Long id) {
        return propietarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Propietario no encontrado"));
    }

    private void aplicar(Propietario propietario, PropietarioRequest request) {
        propietario.setNombre(request.nombre());
        propietario.setApellido(request.apellido());
        propietario.setRut(request.rut());
        propietario.setEmail(request.email());
        propietario.setTelefono(request.telefono());
        propietario.setDireccion(request.direccion());
    }
}
