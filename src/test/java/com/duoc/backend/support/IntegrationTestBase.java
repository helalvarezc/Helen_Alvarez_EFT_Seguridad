package com.duoc.backend.support;

import com.duoc.backend.model.Cita;
import com.duoc.backend.model.EstadoCita;
import com.duoc.backend.model.Mascota;
import com.duoc.backend.model.Propietario;
import com.duoc.backend.model.Rol;
import com.duoc.backend.model.Veterinario;
import com.duoc.backend.repository.CitaRepository;
import com.duoc.backend.repository.FacturaRepository;
import com.duoc.backend.repository.MascotaRepository;
import com.duoc.backend.repository.PropietarioRepository;
import com.duoc.backend.repository.RegistroMedicoRepository;
import com.duoc.backend.repository.UsuarioRepository;
import com.duoc.backend.repository.VeterinarioRepository;
import com.duoc.backend.security.JwtService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public abstract class IntegrationTestBase {

    private static final String SECRETO = generarSecreto();
    private static final AtomicInteger CONTADOR = new AtomicInteger(1);

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected JwtService jwtService;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    @Autowired
    protected UsuarioRepository usuarioRepository;

    @Autowired
    protected PropietarioRepository propietarioRepository;

    @Autowired
    protected MascotaRepository mascotaRepository;

    @Autowired
    protected VeterinarioRepository veterinarioRepository;

    @Autowired
    protected CitaRepository citaRepository;

    @Autowired
    protected RegistroMedicoRepository registroMedicoRepository;

    @Autowired
    protected FacturaRepository facturaRepository;

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry registry) {
        registry.add("app.jwt.secret", () -> SECRETO);
    }

    private static String generarSecreto() {
        byte[] bytes = new byte[48];
        new SecureRandom().nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }

    protected String secretoJwt() {
        return SECRETO;
    }

    protected int siguiente() {
        return CONTADOR.getAndIncrement();
    }

    protected String bearer(Rol rol) {
        String usuario = "test-" + rol.name().toLowerCase(Locale.ROOT);
        return "Bearer " + jwtService.generarToken(usuario, List.of(rol.name()));
    }

    protected String json(Object objeto) throws JsonProcessingException {
        return objectMapper.writeValueAsString(objeto);
    }

    protected ResultActions enviar(HttpMethod metodo, String ruta, Rol rol, Object cuerpo) throws Exception {
        MockHttpServletRequestBuilder solicitud = MockMvcRequestBuilders.request(metodo, ruta)
                .header(HttpHeaders.AUTHORIZATION, bearer(rol));
        if (cuerpo != null) {
            solicitud.contentType(MediaType.APPLICATION_JSON).content(json(cuerpo));
        }
        return mockMvc.perform(solicitud);
    }

    protected Propietario crearPropietario() {
        int n = siguiente();
        Propietario propietario = new Propietario();
        propietario.setNombre("Ana");
        propietario.setApellido("Perez");
        propietario.setRut((10000000 + n) + "-5");
        propietario.setEmail("ana" + n + "@correo.cl");
        propietario.setTelefono("+56911112222");
        propietario.setDireccion("Calle " + n);
        return propietarioRepository.save(propietario);
    }

    protected Mascota crearMascota(Propietario propietario) {
        Mascota mascota = new Mascota();
        mascota.setNombre("Firulais");
        mascota.setEspecie("Perro");
        mascota.setRaza("Labrador");
        mascota.setEdad(5);
        mascota.setPropietario(propietario);
        return mascotaRepository.save(mascota);
    }

    protected Veterinario crearVeterinario() {
        int n = siguiente();
        Veterinario veterinario = new Veterinario();
        veterinario.setNombre("Dr. Soto");
        veterinario.setEspecialidad("Medicina general");
        veterinario.setEmail("vet" + n + "@vet.cl");
        return veterinarioRepository.save(veterinario);
    }

    protected Cita crearCita(Mascota mascota, Veterinario veterinario, LocalDateTime fechaHora) {
        Cita cita = new Cita();
        cita.setFechaHora(fechaHora);
        cita.setMotivo("Control");
        cita.setEstado(EstadoCita.PROGRAMADA);
        cita.setMascota(mascota);
        cita.setVeterinario(veterinario);
        return citaRepository.save(cita);
    }

    protected LocalDateTime fechaFutura() {
        return LocalDateTime.now().plusDays(3).withNano(0).withSecond(0);
    }
}
