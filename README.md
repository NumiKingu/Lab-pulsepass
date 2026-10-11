# PulsePass

**Plataforma de eventos, artistas y entradas** — Taller de capas de persistencia, servicios y controladores REST.

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

Finalmente, se agregó la **capa de controladores REST**, que expone los casos de uso de los Services mediante HTTP
(sin reglas de negocio en los Controllers, solo DTOs y errores centralizados en `GlobalExceptionHandler`).

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
- Spring MVC (API REST)
- Bean Validation
- MockMvc con `@WebMvcTest` y `@MockitoBean`

## API REST

URL base en local: `http://localhost:8080`. Todas las respuestas son JSON.

### Venues

| Método | Endpoint | Descripción | Respuesta exitosa |
|---|---|---|---|
| GET | `/api/venues/{code}` | Consultar venue por código | 200 |
| GET | `/api/venues/active` | Listar venues activos | 200 |

### Eventos

| Método | Endpoint | Descripción | Respuesta exitosa |
|---|---|---|---|
| POST | `/api/events` | Crear evento | 201 |
| GET | `/api/events/{eventCode}` | Consultar evento por código | 200 |
| GET | `/api/events/published` | Listar eventos publicados | 200 |
| PATCH | `/api/events/{eventCode}/publish` | Publicar evento | 200 |
| POST | `/api/events/{eventCode}/artists/{artistId}` | Asociar artista a un evento | 200 |
| GET | `/api/events/by-artist?stageName=...` | Buscar eventos por nombre artístico | 200 |
| GET | `/api/events/{eventCode}/tickets/paid` | Listar tickets pagados de un evento | 200 |

### Artistas

| Método | Endpoint | Descripción | Respuesta exitosa |
|---|---|---|---|
| GET | `/api/artists/{id}` | Consultar artista por ID | 200 |
| GET | `/api/artists/by-stage-name?stageName=...` | Consultar artista por nombre artístico | 200 |
| GET | `/api/artists/active` | Listar artistas activos | 200 |

### Usuarios

| Método | Endpoint | Descripción | Respuesta exitosa |
|---|---|---|---|
| POST | `/api/users` | Registrar usuario | 201 |
| GET | `/api/users/by-email?email=...` | Consultar usuario por email | 200 |
| GET | `/api/users/by-username?username=...` | Consultar usuario por username | 200 |

### Tickets

| Método | Endpoint | Descripción | Respuesta exitosa |
|---|---|---|---|
| POST | `/api/tickets` | Comprar ticket | 201 |
| GET | `/api/tickets/{ticketCode}` | Consultar ticket por código | 200 |
| GET | `/api/tickets/by-user?email=...` | Listar tickets de un usuario | 200 |
| PATCH | `/api/tickets/{ticketCode}/cancel` | Cancelar ticket | 200 |
| PATCH | `/api/tickets/{ticketCode}/use` | Marcar ticket como usado | 200 |

### Códigos HTTP y manejo de errores

| Código | Cuándo se devuelve |
|---|---|
| 200 OK | Consultas y actualizaciones correctas |
| 201 Created | Creación de evento, usuario o ticket |
| 400 Bad Request | Request inválido (Bean Validation), JSON mal formado, parámetro faltante o con formato inválido |
| 404 Not Found | Recurso inexistente (`ResourceNotFoundException`) |
| 409 Conflict | Duplicado (`DuplicateResourceException`) o regla de negocio incumplida (`BusinessRuleException`) |
| 500 Internal Server Error | Error inesperado (sin exponer detalles internos) |

Todos los errores usan el contrato `ErrorResponse` (`timestamp`, `status`, `error`, `message`, `details`).

## Pruebas

Las pruebas de Controller usan `@WebMvcTest`, `MockMvc` y `@MockitoBean` (Services mockeados), por lo que no requieren
PostgreSQL ni Docker. Están en `src/test/java/com/pulsepass/controller/`: `VenueControllerTest`, `EventControllerTest`,
`ArtistControllerTest`, `UserControllerTest` y `TicketControllerTest`.

```bash
# Solo pruebas de Controller
mvn clean test -Dtest="*ControllerTest"

# Todo el proyecto (los tests *IT de persistencia requieren Docker)
mvn clean test
```