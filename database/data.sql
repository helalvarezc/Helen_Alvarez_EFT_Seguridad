INSERT INTO propietario (nombre, apellido, rut, email, telefono, direccion) VALUES
('Juan', 'Perez', '12345678-5', 'juan.perez@correo.cl', '+56911112222', 'Av. Principal 100, Santiago'),
('Ana', 'Gomez', '9876543-2', 'ana.gomez@correo.cl', '+56933334444', 'Calle Los Olmos 45, Providencia');

INSERT INTO mascota (nombre, especie, raza, edad, propietario_id) VALUES
('Firulais', 'Perro', 'Labrador', 5, 1),
('Michi', 'Gato', 'Siames', 3, 2);

INSERT INTO veterinario (nombre, especialidad, email) VALUES
('Carlos Soto', 'Medicina general', 'carlos.soto@veterinaria.cl'),
('Marta Mena', 'Cirugia', 'marta.mena@veterinaria.cl');
