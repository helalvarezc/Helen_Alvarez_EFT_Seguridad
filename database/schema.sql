CREATE TABLE usuario (
    id BIGINT NOT NULL AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL,
    email VARCHAR(120) NOT NULL,
    password VARCHAR(100) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    rol VARCHAR(20) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_usuario_username UNIQUE (username),
    CONSTRAINT uk_usuario_email UNIQUE (email)
);

CREATE TABLE propietario (
    id BIGINT NOT NULL AUTO_INCREMENT,
    nombre VARCHAR(100) NOT NULL,
    apellido VARCHAR(100) NOT NULL,
    rut VARCHAR(12) NOT NULL,
    email VARCHAR(120) NOT NULL,
    telefono VARCHAR(20) NOT NULL,
    direccion VARCHAR(200) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_propietario_rut UNIQUE (rut)
);

CREATE TABLE mascota (
    id BIGINT NOT NULL AUTO_INCREMENT,
    nombre VARCHAR(80) NOT NULL,
    especie VARCHAR(40) NOT NULL,
    raza VARCHAR(80) NOT NULL,
    edad INT NOT NULL,
    propietario_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_mascota_propietario FOREIGN KEY (propietario_id) REFERENCES propietario (id)
);

CREATE TABLE veterinario (
    id BIGINT NOT NULL AUTO_INCREMENT,
    nombre VARCHAR(100) NOT NULL,
    especialidad VARCHAR(100) NOT NULL,
    email VARCHAR(120) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_veterinario_email UNIQUE (email)
);

CREATE TABLE cita (
    id BIGINT NOT NULL AUTO_INCREMENT,
    fecha_hora DATETIME NOT NULL,
    motivo VARCHAR(255) NOT NULL,
    estado VARCHAR(20) NOT NULL,
    mascota_id BIGINT NOT NULL,
    veterinario_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_cita_mascota FOREIGN KEY (mascota_id) REFERENCES mascota (id),
    CONSTRAINT fk_cita_veterinario FOREIGN KEY (veterinario_id) REFERENCES veterinario (id)
);

CREATE INDEX idx_cita_veterinario_fecha ON cita (veterinario_id, fecha_hora);

CREATE TABLE registro_medico (
    id BIGINT NOT NULL AUTO_INCREMENT,
    fecha DATE NOT NULL,
    diagnostico VARCHAR(1000) NOT NULL,
    tratamiento VARCHAR(1000) NOT NULL,
    mascota_id BIGINT NOT NULL,
    veterinario_id BIGINT NOT NULL,
    cita_id BIGINT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_registro_mascota FOREIGN KEY (mascota_id) REFERENCES mascota (id),
    CONSTRAINT fk_registro_veterinario FOREIGN KEY (veterinario_id) REFERENCES veterinario (id),
    CONSTRAINT fk_registro_cita FOREIGN KEY (cita_id) REFERENCES cita (id)
);

CREATE TABLE factura (
    id BIGINT NOT NULL AUTO_INCREMENT,
    fecha DATETIME NOT NULL,
    total DECIMAL(12,2) NOT NULL,
    estado VARCHAR(20) NOT NULL,
    propietario_id BIGINT NOT NULL,
    cita_id BIGINT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_factura_propietario FOREIGN KEY (propietario_id) REFERENCES propietario (id),
    CONSTRAINT fk_factura_cita FOREIGN KEY (cita_id) REFERENCES cita (id)
);

CREATE TABLE detalle_factura (
    id BIGINT NOT NULL AUTO_INCREMENT,
    descripcion VARCHAR(200) NOT NULL,
    cantidad INT NOT NULL,
    precio_unitario DECIMAL(12,2) NOT NULL,
    factura_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_detalle_factura FOREIGN KEY (factura_id) REFERENCES factura (id) ON DELETE CASCADE
);
