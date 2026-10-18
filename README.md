# PulsePass

**Plataforma de eventos, artistas y entradas** — Taller de capa de persistencia.

## Integrantes

- Cesar Andres Acosta Torres
- Camilo Andres Hincapie Gomez

## Contexto del proyecto

PulsePass es un caso de estudio académico que modela una plataforma para descubrir eventos y administrar 
entradas de conciertos, festivales, conferencias y otras actividades. El proyecto se centra en 
la **capa de persistencia**: el modelo relacional (venues, eventos, artistas, usuarios, perfiles y tickets), 
las migraciones con Flyway, las entidades JPA, los repositories de Spring Data y las consultas (Query Methods
y JPQL).

La integridad de los datos se garantiza en PostgreSQL mediante restricciones `UNIQUE`, `CHECK` y llaves foráneas, 
y todo se valida con pruebas de integración contra una base de datos real usando Testcontainers.

Sobre esa base, se incorporó la **capa de servicios**, encargada de orquestar la lógica de negocio: validación de 
reglas (unicidad de usuarios y eventos, estados válidos de tickets y eventos, capacidad de los venues, edad mínima 
requerida), cálculo de precios según el tipo de entrada, y la conversión entre entidades JPA y DTOs 
(`request`/`response`) mediante MapStruct. Cada entidad de dominio cuenta con su correspondiente `Service` 
(interfaz) e implementación, que coordinan los repositories y lanzan excepciones específicas 
(`ResourceNotFoundException`, `DuplicateResourceException`, `BusinessRuleException`) cuando una operación 
viola una regla del negocio.

## Tecnologías

- Java 21
- Spring Boot 4 (Maven)
- Spring Data JPA / Hibernate
- PostgreSQL
- Flyway
- JUnit 5 y Testcontainers
- MapStruct
- AssertJ
- Mockito

## Seguridad (Fase 4)

API **stateless** con JWT firmado **HS256** (JJWT 0.13) enviado en `Authorization: Bearer <token>`.
Duración del token: 900 s (15 min). No hay refresh tokens ni revocación inmediata: el logout solo borra la copia del cliente.

### Variables de entorno

| Variable | Obligatoria | Descripción |
|---|---|---|
| `PULSEPASS_JWT_SECRET_BASE64` | Sí | Clave HMAC en Base64, mínimo 32 bytes aleatorios (`openssl rand -base64 32`). Sin ella la app no arranca. |
| `PULSEPASS_JWT_ISSUER` | No | Emisor esperado (por defecto `pulsepass-api`). |
| `PULSEPASS_JWT_TTL` | No | Duración ISO-8601 (por defecto `PT15M`). |
| `PULSEPASS_CORS_ALLOWED_ORIGINS` | No | Orígenes permitidos separados por coma. |
| `PULSEPASS_SEED_ADMIN_PASSWORD` / `PULSEPASS_SEED_STAFF_PASSWORD` | No | Solo perfil `dev`: crea `miguel` (ADMIN) y `laura` (STAFF). |


### Roles

| Rol | Puede |
|---|---|
| Anónimo | Ver venues, artistas y eventos publicados; registrarse; iniciar sesión |
| `USER` | Comprar entradas para sí mismo; ver/cancelar sus tickets; ver sus datos |
| `STAFF` | Ver tickets pagados de un evento; marcar tickets como usados |
| `ADMIN` | Crear/publicar eventos, asociar artistas, consultar datos de otros usuarios |



### Endpoints nuevos

| Método | Endpoint | Acceso | Respuesta |
|---|---|---|---|
| POST | `/api/auth/login` | Público | 200 `AuthResponse(accessToken, tokenType, expiresIn)` |
| GET | `/api/auth/me` | Autenticado | 200 `AuthenticatedUserResponse(id, username, roles)` |

`
### Errores de seguridad

| Código | Cuándo |
|---|---|
| 401 | Sin token, token inválido/expirado/manipulado, login incorrecto o usuario inactivo |
| 403 | Rol insuficiente o recurso de otro usuario |


### Limitaciones conocidas del MVP

- `STAFF` opera sobre tickets de cualquier evento (aún no existe asignación de personal a eventos).
- Sin límite de intentos de login, rotación de claves ni revocación por `jti`.
- Usuarios creados antes de la Fase 4 no tienen contraseña y no pueden iniciar sesión hasta una activación segura.


