-- =====================================================================
-- seed-e2e.sql — Fixtures EXCLUSIVOS del ambiente E2E (pulsepass_e2e)
-- Ejecutar DESPUES de arrancar la app una vez (Flyway ya creo las tablas).
-- Idempotente: se puede ejecutar varias veces sin duplicar datos.
-- No contiene contraseñas: ADMIN (miguel) y STAFF (laura) los crea
-- DevSecurityDataSeeder con el perfil dev y BCrypt.
-- =====================================================================

-- Proteccion: aborta si no estas conectado a la base E2E
DO $$
BEGIN
    IF current_database() <> 'pulsepass_e2e' THEN
        RAISE EXCEPTION 'seed-e2e.sql solo se ejecuta sobre pulsepass_e2e (estas en %)', current_database();
    END IF;
END $$;

-- Venue activo principal (eventos normales)
INSERT INTO venues (code, name, city, address, capacity, active)
VALUES ('VEN-E2E-01', 'E2E Arena Santa Marta', 'Santa Marta', 'Calle E2E # 1-01', 500, TRUE)
ON CONFLICT (code) DO NOTHING;

-- Venue activo de capacidad 1 (prueba de aforo agotado)
INSERT INTO venues (code, name, city, address, capacity, active)
VALUES ('VEN-E2E-MINI', 'E2E Sala Mini', 'Santa Marta', 'Calle E2E # 1-02', 1, TRUE)
ON CONFLICT (code) DO NOTHING;

-- Venue INACTIVO (no debe aparecer en /active ni aceptar eventos)
INSERT INTO venues (code, name, city, address, capacity, active)
VALUES ('VEN-E2E-OFF', 'E2E Venue Cerrado', 'Santa Marta', 'Calle E2E # 1-03', 300, FALSE)
ON CONFLICT (code) DO NOTHING;

-- Verificacion rapida
SELECT code, capacity, active FROM venues WHERE code LIKE 'VEN-E2E-%' ORDER BY code;
SELECT u.username, r.role
FROM users u JOIN user_roles r ON r.user_id = u.id
WHERE u.username IN ('miguel', 'laura');
