package com.duoc.backend.controller;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.duoc.backend.model.Rol;
import com.duoc.backend.support.IntegrationTestBase;
import java.util.Map;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpMethod;

class RolesIntegrationTest extends IntegrationTestBase {

    @ParameterizedTest
    @CsvSource({
            "RECEPCIONISTA,DELETE,/propietarios/1",
            "VETERINARIO,DELETE,/citas/1",
            "RECEPCIONISTA,GET,/usuarios",
            "VETERINARIO,GET,/usuarios",
            "RECEPCIONISTA,GET,/registros-medicos",
            "RECEPCIONISTA,POST,/registros-medicos",
            "VETERINARIO,GET,/facturas",
            "VETERINARIO,POST,/facturas",
            "VETERINARIO,PATCH,/facturas/1/pagar",
            "VETERINARIO,POST,/propietarios",
            "VETERINARIO,POST,/mascotas",
            "VETERINARIO,POST,/citas",
            "VETERINARIO,PUT,/citas/1",
            "RECEPCIONISTA,POST,/veterinarios",
            "RECEPCIONISTA,PUT,/veterinarios/1"
    })
    void accesoDenegadoPorRol(Rol rol, String metodo, String ruta) throws Exception {
        enviar(HttpMethod.valueOf(metodo), ruta, rol, Map.of()).andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @CsvSource({
            "ADMIN,GET,/usuarios",
            "ADMIN,DELETE,/propietarios/999999",
            "ADMIN,POST,/veterinarios",
            "ADMIN,GET,/facturas",
            "RECEPCIONISTA,GET,/propietarios",
            "RECEPCIONISTA,POST,/propietarios",
            "RECEPCIONISTA,GET,/veterinarios",
            "RECEPCIONISTA,POST,/facturas",
            "RECEPCIONISTA,GET,/facturas",
            "RECEPCIONISTA,PATCH,/citas/1/estado",
            "VETERINARIO,GET,/mascotas",
            "VETERINARIO,GET,/citas",
            "VETERINARIO,POST,/registros-medicos",
            "VETERINARIO,GET,/registros-medicos",
            "VETERINARIO,PATCH,/citas/1/estado"
    })
    void accesoPermitidoPorRol(Rol rol, String metodo, String ruta) throws Exception {
        int codigo = enviar(HttpMethod.valueOf(metodo), ruta, rol, Map.of())
                .andReturn().getResponse().getStatus();
        assertNotEquals(401, codigo);
        assertNotEquals(403, codigo);
    }
}
