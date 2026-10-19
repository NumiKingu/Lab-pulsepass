# Registro de ejecución manual — PulsePass E2E

| Campo | Valor |
|---|---|
| Ejecutado por |Camilo Hincapie y Cesar Acosta|
| Fecha |18/10/2026|
| runId |01|
|Rama probada|feature/pruebaE2E|
| Base de datos | pulsepass_e2e |

Resultado: `PASS` / `FAIL` / `BLOCKED` / `NOT RUN`. No pegar tokens, contraseñas ni datos personales reales.

| # | Caso | Actor | Ruta | HTTP esperado | HTTP observado | JSON / estado observado | Resultado | Fecha/hora | Observación |
|---|---|---|---|---|---|---|---|---|---|
| 001 | PRE-01 | ANONYMOUS | `GET /api/venues/active` | 200 | 200 | El arreglo contiene VEN-E2E-01 y VEN-E2E-MINI | PASS |18/10/2005 |sin observacion |
| 002 | TC-BIZ-001a | ANONYMOUS | `GET /api/venues/active` | 200 | 200 | Incluye VEN-E2E-01 y VEN-E2E-MINI con active=true. | PASS |18/10/2025 | |
| 003 | TC-BIZ-001b | ANONYMOUS | `GET /api/venues/{{venueActiveCode}}` | 200 | 200 | code = VEN-E2E-01, capacity = 500, active = true. | PASS | | |
| 004 | TC-HTTP-VEN-404 | ANONYMOUS | `GET /api/venues/NO-EXISTE-{{runId}}` | 404 | 404 | Not Found | PASS | | |
| 005 | TC-BIZ-002a | ANONYMOUS | `GET /api/artists/active` | 200 | 200 | Lista ordenada por stageName con los 5 artistas de la migración V2 (Caribbean Sound, Digital Pulse, Neon Waves, Ocean Drive, Solar Beat). | PASS | | |
| 006 | TC-BIZ-002b | ANONYMOUS | `GET /api/artists/{{artistId}}` | 200 | 200 | stageName = Solar Beat, genre = Electronic, active = true. | PASS | | |
| 007 | TC-HTTP-ART-404 | ANONYMOUS | `GET /api/artists/999999` | 404 | 404 | ErrorResponse con 5 campos (timestamp, status, error, message, details); status igual al HTTP; sin stack trace, hash, token ni secretos. | PASS | | |
| 008 | TC-HTTP-ART-400 | ANONYMOUS | `GET /api/artists/abc` | 400 | 400 | message "Invalid request parameter", details.id = "Invalid value: abc". | PASS | | |
| 009 | TC-BIZ-002c | ANONYMOUS | `GET /api/artists/by-stage-name?stageName={{artistStageName}}` | 200 | 200 | Mismo id que en TC-BIZ-002a (consulta consistente). | PASS | | |
| 010 | TC-HTTP-ART-404b | ANONYMOUS | `GET /api/artists/by-stage-name?stageName=Nadie {{runId}}` | 404 | 404 | message "Artist not found: Nadie {{runId}}". | PASS | | |
| 011 | TC-AUTH-001 | ANONYMOUS | `POST /api/users` | 201 | 201 | username/email iguales a los enviados, active = true. | PASS | | |
| 012 | TC-BIZ-006 | ANONYMOUS | `POST /api/users` | 201 | 201 | Identidad distinta a USER_A (id, username y email diferentes). | PASS | | |
| 013 | TC-SEC-016 | ANONYMOUS | `POST /api/users` | 201 | 201 | 201: el campo role se ignora (no existe en RegisterUserRequest). Un 400 también es aceptable: lo importante es que NO obtenga ADMIN. | PASS | | |
| 014 | TC-BIZ-006b | ANONYMOUS | `POST /api/users` | 409 | 409 | status 409, error "Conflict", message "Username already exists: …". | PASS | | |
| 015 | TC-HTTP-USR-400 | ANONYMOUS | `POST /api/users` | 400 | 400 | details.email = "Email format is invalid" y details.password = "Password must have between 12 and 72 characters". | PASS | | |
| 016 | TC-HTTP-JSON-400 | ANONYMOUS | `POST /api/users` | 400 | 400 | message "Malformed or invalid JSON request", details.body presente. | PASS | | |
| 017 | TC-AUTH-002 | ANONYMOUS | `POST /api/auth/login` | 200 | 200 | tokenType = "Bearer", expiresIn = {{expectedTokenTtlSeconds}} (900), accessToken no vacío. | PASS | | |
| 018 | TC-AUTH-003 | ANONYMOUS | `POST /api/auth/login` | 200 | 200 | 200 con un token nuevo | PASS | | |
| 019 | TC-AUTH-LOGIN-B | ANONYMOUS | `POST /api/auth/login` | 200 | 200 | tokenType Bearer, expiresIn 900. | PASS | | |
| 020 | TC-AUTH-LOGIN-C | ANONYMOUS | `POST /api/auth/login` | 200 | 200 | tokenType Bearer, expiresIn 900. | PASS | | |
| 021 | TC-AUTH-LOGIN-ADMIN | ANONYMOUS | `POST /api/auth/login` | 200 | 200 | Creo el token ADMIN | PASS | | |
| 022 | TC-AUTH-LOGIN-STAFF | ANONYMOUS | `POST /api/auth/login` | 200 | 200 | Creo el token STAFF | PASS | | |
| 023 | TC-AUTH-004 | ANONYMOUS | `POST /api/auth/login` | 401 | 401 | "message": "Credenciales invalidas o autenticacion requerida" | PASS | | |
| 024 | TC-AUTH-005 | ANONYMOUS | `POST /api/auth/login` | 401 | 401 | Mismo status y MISMO message que TC-AUTH-004: no se puede saber si el usuario existe. | PASS | | |
| 025 | TC-HTTP-LOGIN-400 | ANONYMOUS | `POST /api/auth/login` | 400 | 400 | details.identifier = "Identifier is required" y details.password = "Password is required". | PASS | | |
| 026 | TC-AUTH-006 | USER_A | `GET /api/auth/me` | 200 | 200 | Sin password, hash ni token en el cuerpo. | PASS | | |
| 027 | TC-AUTH-ME-B | USER_B | `GET /api/auth/me` | 200 | 200 | username = {{userBUsername}}, roles = ["USER"]; id distinto al de USER_A. | PASS | | |
| 028 | TC-SEC-016b | USER_C | `GET /api/auth/me` | 200 | 200 | roles = ["USER"] exactamente. Si aparece ADMIN  | PASS | | |
| 029 | TC-AUTH-ME-ADMIN | ADMIN | `GET /api/auth/me` | 200 | 200 | username = {{adminIdentifier}}, roles = ["ADMIN"]. | PASS | | |
| 030 | TC-AUTH-ME-STAFF | STAFF | `GET /api/auth/me` | 200 | 200 | username = {{staffIdentifier}}, roles = ["STAFF"]. | PASS | | |
| 031 | TC-AUTH-007 | ANONYMOUS | `GET /api/auth/me` | 401 | 401 | status 401, error "Unauthorized", "message": "Credenciales invalidas o autenticacion requerida". | PASS | | |
| 032 | TC-AUTH-008 | ANONYMOUS | `GET /api/auth/me` | 401 | 401 | 401 + ErrorResponse; nunca 500. | PASS | | |
| 033 | TC-AUTH-009 | ANONYMOUS | `GET /api/auth/me` | 401 | 401 | 401: la firma ya no coincide, no entra al controller. | PASS| | |
| 034 | TC-AUTH-011 | ANONYMOUS | `GET /api/auth/me` | 401 | 401 | 401 esperado | PASS | | |
| 035 | TC-AUTH-011b | ANONYMOUS | `GET /api/auth/me` | 401 | 401 | 401: los tokens sin firma se rechazan siempre. | PASS | | |
| 036 | TC-AUTH-012 | ANONYMOUS | `GET /api/auth/me?access_token={{userAToken}}` | 401 | 401 | 401: el token en la URL se ignora (solo vale el header Authorization). | PASS | | |
| 037 | TC-AUTH-015 | ANONYMOUS | `GET /api/events/published` | 401 | 401 | si llega un Bearer inválido se responde 401 aunque la ruta sea pública; nunca se autentica por accidente. | PASS | | |
| 038 | TC-SEC-001a | ANONYMOUS | `POST /api/events` | 401 | 401 | El evento NO se crea | PASS | | |
| 039 | TC-SEC-001b | USER_A | `POST /api/events` | 403 | 403 | status 403, error "Forbidden", "message": "No tiene permisos para realizar esta operacion". | PASS | | |
| 040 | TC-SEC-001c | STAFF | `POST /api/events` | 403 | 403 | 403: STAFF no administra el catálogo. | PASS | | |
| 041 | TC-BIZ-003 | ADMIN | `POST /api/events` | 201 | 201 | eventCode = {{publishedEventCode}}, status = "DRAFT", venueCode = VEN-E2E-01, artists = []. | PASS | | |
| 042 | TC-BIZ-003b | ADMIN | `POST /api/events` | 201 | 201 | status = "DRAFT". Este evento NUNCA se publica. | PASS | | |
| 043 | TC-BIZ-003c | ADMIN | `POST /api/events` | 201 | 201 | venueCode = VEN-E2E-MINI, status DRAFT. Servirá para probar aforo agotado. | PASS | | |
| 044 | TC-BIZ-003d | ADMIN | `POST /api/events` | 201 | 201 | minimumAge = 99. Servirá para probar edad insuficiente. | PASS | | |
| 045 | TC-BIZ-015a | ADMIN | `POST /api/events` | 409 | 409 | message "Event code already exists: …". | PASS | | |
| 046 | TC-BIZ-015b | ADMIN | `POST /api/events` | 409 | 409 | message "Venue is not active: VEN-E2E-OFF". | PASS | | |
| 047 | TC-BIZ-015c | ADMIN | `POST /api/events` | 404 | 404 | message "Venue not found: NO-EXISTE". | PASS | | |
| 048 | TC-BIZ-015d | ADMIN | `POST /api/events` | 409 | 409 | message "Event date must be in the future.". | PASS | | |
| 049 | TC-BIZ-015e | ADMIN | `POST /api/events` | 400 | 400 | message "Malformed or invalid JSON request" (OPERA no existe en EventCategory). | PASS | | |
| 050 | TC-BIZ-015f | ADMIN | `POST /api/events` | 400 | 400 | message "Validation failed"; details incluye eventCode, name, category, eventDate, minimumAge y venueCode. | PASS | | |
| 051 | TC-SEC-015 | ADMIN | `GET /api/events/{{draftEventCode}}` | 200 | 200 | status = "DRAFT" y datos completos del evento. | PASS | | |
| 052 | TC-SEC-015b | STAFF | `GET /api/events/{{draftEventCode}}` | 200 | 200 | 200: STAFF/ADMIN sí pueden ver borradores. | PASS | | |
| 053 | TC-SEC-004 | ANONYMOUS | `GET /api/events/{{draftEventCode}}` | 403 | 403 | 403 (política fija para recursos no visibles: expectedForeignResourceStatus). | PASS | | |
| 054 | TC-SEC-004b | USER_A | `GET /api/events/{{draftEventCode}}` | 403 | 403 | 403 sin datos del borrador. | PASS | | |
| 055 | TC-SEC-003 | USER_A | `POST /api/events/{{publishedEventCode}}/artists/{{artistId}}` | 403 | 403 | Arroja que no tiene permisos | PASS | | |
| 056 | TC-BIZ-005 | ADMIN | `POST /api/events/{{publishedEventCode}}/artists/{{artistId}}` | 200 | 200 | artists contiene exactamente 1 elemento con id = {{artistId}} y stageName Solar Beat. | PASS | | |
| 057 | TC-BIZ-005b | ADMIN | `POST /api/events/{{draftEventCode}}/artists/{{artistId}}` | 200 | 200 | Se asocia correctamente | PASS | | |
| 058 | TC-BIZ-005c | ADMIN | `POST /api/events/{{publishedEventCode}}/artists/{{artistId}}` | 409 | 409 | message "Artist Solar Beat is already associated with event …". | PASS | | |
| 059 | TC-BIZ-005d | ADMIN | `POST /api/events/{{publishedEventCode}}/artists/999999` | 404 | 404 | message "Artist not found: 999999". | PASS | | |
| 060 | TC-BIZ-004a | ANONYMOUS | `GET /api/events/published` | 200 | 200 | Ningún elemento tiene eventCode {{publishedEventCode}}, {{draftEventCode}}, {{miniEventCode}} ni {{ageEventCode}} (todos en DRAFT). | PASS | | |
| 061 | TC-SEC-002a | USER_A | `PATCH /api/events/{{publishedEventCode}}/publish` | 403 | 403 | 403 + ErrorResponse. | PASS | | |
| 062 | TC-SEC-002b | STAFF | `PATCH /api/events/{{publishedEventCode}}/publish` | 403 | 403 | 403 + ErrorResponse. | PASS | | |
| 063 | TC-BIZ-004 | ADMIN | `PATCH /api/events/{{publishedEventCode}}/publish` | 200 | 200 | status = "PUBLISHED" y conserva el artista asociado. | PASS | | |   
| 064 | TC-BIZ-004b | ADMIN | `PATCH /api/events/{{publishedEventCode}}/publish` | 409 | 409 | message "Only DRAFT events can be published. Current status: PUBLISHED". | PASS | | |
| 065 | TC-BIZ-004c | ADMIN | `PATCH /api/events/{{miniEventCode}}/publish` | 200 | 200 | status = "PUBLISHED". | PASS | | |
| 066 | TC-BIZ-004d | ADMIN | `PATCH /api/events/{{ageEventCode}}/publish` | 200 | pendiente | pendiente | NOT RUN | | |
| 067 | TC-BIZ-004e | ANONYMOUS | `GET /api/events/{{publishedEventCode}}` | 200 | pendiente | pendiente | NOT RUN | | |
| 068 | TC-BIZ-004f | ANONYMOUS | `GET /api/events/published` | 200 | pendiente | pendiente | NOT RUN | | |
| 069 | TC-SEC-005 | ANONYMOUS | `GET /api/events/by-artist?stageName={{artistStageName}}` | 200 | pendiente | pendiente | NOT RUN | | |
| 070 | TC-BIZ-005e | ADMIN | `GET /api/events/by-artist?stageName={{artistStageName}}` | 200 | pendiente | pendiente | NOT RUN | | |
| 071 | TC-HTTP-EVT-404 | ANONYMOUS | `GET /api/events/NO-EXISTE-{{runId}}` | 404 | pendiente | pendiente | NOT RUN | | |
| 072 | TC-HTTP-EVT-404b | ANONYMOUS | `GET /api/events/by-artist?stageName=Nadie` | 404 | pendiente | pendiente | NOT RUN | | |
| 073 | TC-SEC-008 | USER_A | `POST /api/tickets` | 403 | pendiente | pendiente | NOT RUN | | |
| 074 | TC-SEC-008c | ANONYMOUS | `POST /api/tickets` | 401 | pendiente | pendiente | NOT RUN | | |
| 075 | TC-SEC-008d | STAFF | `POST /api/tickets` | 403 | pendiente | pendiente | NOT RUN | | |
| 076 | TC-BIZ-007 | USER_A | `POST /api/tickets` | 201 | pendiente | pendiente | NOT RUN | | |
| 077 | TC-BIZ-007b | USER_A | `POST /api/tickets` | 201 | pendiente | pendiente | NOT RUN | | |
| 078 | TC-BIZ-007c | USER_B | `POST /api/tickets` | 201 | pendiente | pendiente | NOT RUN | | |
| 079 | TC-HTTP-TKT-400 | USER_A | `POST /api/tickets` | 400 | pendiente | pendiente | NOT RUN | | |
| 080 | TC-HTTP-TKT-400b | USER_A | `POST /api/tickets` | 400 | pendiente | pendiente | NOT RUN | | |
| 081 | TC-BIZ-014a | USER_A | `POST /api/tickets` | 409 | pendiente | pendiente | NOT RUN | | |
| 082 | TC-BIZ-014b | USER_A | `POST /api/tickets` | 409 | pendiente | pendiente | NOT RUN | | |
| 083 | TC-BIZ-014c | USER_A | `POST /api/tickets` | 404 | pendiente | pendiente | NOT RUN | | |
| 084 | TC-BIZ-014d | USER_A | `POST /api/tickets` | 201 | pendiente | pendiente | NOT RUN | | |
| 085 | TC-BIZ-014e | USER_B | `POST /api/tickets` | 409 | pendiente | pendiente | NOT RUN | | |
| 086 | TC-BIZ-014f | ADMIN | `GET /api/events/{{miniEventCode}}` | 200 | pendiente | pendiente | NOT RUN | | |
| 087 | TC-BIZ-008 | USER_A | `GET /api/tickets/{{ticketCancelableCode}}` | 200 | pendiente | pendiente | NOT RUN | | |
| 088 | TC-SEC-009 | USER_B | `GET /api/tickets/{{ticketCancelableCode}}` | 403 | pendiente | pendiente | NOT RUN | | |
| 089 | TC-BIZ-008b | STAFF | `GET /api/tickets/{{ticketCancelableCode}}` | 200 | pendiente | pendiente | NOT RUN | | |
| 090 | TC-SEC-009b | ANONYMOUS | `GET /api/tickets/{{ticketCancelableCode}}` | 401 | pendiente | pendiente | NOT RUN | | |
| 091 | TC-SEC-009c | USER_A | `GET /api/tickets/TCK-NOEXISTE` | 403 | pendiente | pendiente | NOT RUN | | |
| 092 | TC-HTTP-TKT-404 | STAFF | `GET /api/tickets/TCK-NOEXISTE` | 404 | pendiente | pendiente | NOT RUN | | |
| 093 | TC-BIZ-008c | USER_A | `GET /api/tickets/by-user?email={{userAEmail}}` | 200 | pendiente | pendiente | NOT RUN | | |
| 094 | TC-SEC-010 | USER_B | `GET /api/tickets/by-user?email={{userAEmail}}` | 403 | pendiente | pendiente | NOT RUN | | |
| 095 | TC-BIZ-008d | ADMIN | `GET /api/tickets/by-user?email={{userAEmail}}` | 200 | pendiente | pendiente | NOT RUN | | |
| 096 | TC-SEC-008b | USER_B | `GET /api/tickets/by-user?email={{userBEmail}}` | 200 | pendiente | pendiente | NOT RUN | | |
| 097 | TC-BIZ-USR-02 | USER_A | `GET /api/users/by-email?email={{userAEmail}}` | 200 | pendiente | pendiente | NOT RUN | | |
| 098 | TC-SEC-006 | USER_B | `GET /api/users/by-email?email={{userAEmail}}` | 403 | pendiente | pendiente | NOT RUN | | |
| 099 | TC-BIZ-USR-02b | ADMIN | `GET /api/users/by-email?email={{userAEmail}}` | 200 | pendiente | pendiente | NOT RUN | | |
| 100 | TC-HTTP-USR-400b | USER_A | `GET /api/users/by-email` | 400 | pendiente | pendiente | NOT RUN | | |
| 101 | TC-BIZ-USR-03 | USER_A | `GET /api/users/by-username?username={{userAUsername}}` | 200 | pendiente | pendiente | NOT RUN | | |
| 102 | TC-SEC-007 | USER_B | `GET /api/users/by-username?username={{userAUsername}}` | 403 | pendiente | pendiente | NOT RUN | | |
| 103 | TC-SEC-007b | ANONYMOUS | `GET /api/users/by-username?username={{userAUsername}}` | 401 | pendiente | pendiente | NOT RUN | | |
| 104 | TC-SEC-011 | USER_B | `PATCH /api/tickets/{{ticketCancelableCode}}/cancel` | 403 | pendiente | pendiente | NOT RUN | | |
| 105 | TC-SEC-012 | USER_B | `PATCH /api/tickets/{{ticketUsableCode}}/use` | 403 | pendiente | pendiente | NOT RUN | | |
| 106 | TC-SEC-012b | USER_A | `PATCH /api/tickets/{{ticketUsableCode}}/use` | 403 | pendiente | pendiente | NOT RUN | | |
| 107 | TC-SEC-011b | USER_A | `GET /api/tickets/by-user?email={{userAEmail}}` | 200 | pendiente | pendiente | NOT RUN | | |
| 108 | TC-SEC-013 | USER_A | `GET /api/events/{{publishedEventCode}}/tickets/paid` | 403 | pendiente | pendiente | NOT RUN | | |
| 109 | TC-SEC-013b | ANONYMOUS | `GET /api/events/{{publishedEventCode}}/tickets/paid` | 401 | pendiente | pendiente | NOT RUN | | |
| 110 | TC-BIZ-009 | STAFF | `GET /api/events/{{publishedEventCode}}/tickets/paid` | 200 | pendiente | pendiente | NOT RUN | | |
| 111 | TC-BIZ-010 | USER_A | `PATCH /api/tickets/{{ticketCancelableCode}}/cancel` | 200 | pendiente | pendiente | NOT RUN | | |
| 112 | TC-BIZ-010b | USER_A | `GET /api/tickets/{{ticketCancelableCode}}` | 200 | pendiente | pendiente | NOT RUN | | |
| 113 | TC-BIZ-011 | USER_A | `PATCH /api/tickets/{{ticketCancelableCode}}/cancel` | 409 | pendiente | pendiente | NOT RUN | | |
| 114 | TC-BIZ-009b | STAFF | `GET /api/events/{{publishedEventCode}}/tickets/paid` | 200 | pendiente | pendiente | NOT RUN | | |
| 115 | TC-SEC-014 | STAFF | `PATCH /api/tickets/{{ticketUsableCode}}/use` | 200 | pendiente | pendiente | NOT RUN | | |
| 116 | TC-BIZ-012 | USER_A | `GET /api/tickets/{{ticketUsableCode}}` | 200 | pendiente | pendiente | NOT RUN | | |
| 117 | TC-BIZ-013 | STAFF | `PATCH /api/tickets/{{ticketUsableCode}}/use` | 409 | pendiente | pendiente | NOT RUN | | |
| 118 | TC-SEC-011c | STAFF | `PATCH /api/tickets/{{ticketUserBCode}}/cancel` | 403 | pendiente | pendiente | NOT RUN | | |
| 119 | TC-BIZ-010c | ADMIN | `PATCH /api/tickets/{{ticketUserBCode}}/cancel` | 200 | pendiente | pendiente | NOT RUN | | |
| 120 | TC-AUTH-013 | USER_C | `GET /api/auth/me` | 401 | pendiente | pendiente | NOT RUN | | |
| 121 | TC-AUTH-013b | ANONYMOUS | `POST /api/auth/login` | 401 | pendiente | pendiente | NOT RUN | | |
| 122 | TC-SEC-017 | STAFF | `GET /api/events/{{publishedEventCode}}/tickets/paid` | 403 | pendiente | pendiente | NOT RUN | | |
| 123 | TC-SEC-018 | ADMIN | `DELETE /api/events/{{publishedEventCode}}` | 403 | pendiente | pendiente | NOT RUN | | |
| 124 | TC-SEC-018b | ANONYMOUS | `DELETE /api/events/{{publishedEventCode}}` | 401 | pendiente | pendiente | NOT RUN | | |
| 125 | TC-AUTH-010 | ANONYMOUS | `GET /api/auth/me` | 401 | pendiente | pendiente | NOT RUN | | |

## Consolidación

| Módulo | PASS | FAIL | BLOCKED | NOT RUN |
|---|---|---|---|---|
| 00 Preparación / conectividad | | | | |
| 01 Venues (2 rutas) | | | | |
| 02 Artists (3 rutas) | | | | |
| 03 Auth / usuarios (registro, login, me) | | | | |
| 04 Usuarios / consulta y propiedad (2 rutas) | | | | |
| 05 Events / administración y visibilidad (6 rutas) | | | | |
| 06 Tickets / compra, consulta y ciclo de vida (6 rutas) | | | | |
| 07 Seguridad / escenarios negativos | | | | |
| 08 Validaciones y errores HTTP | | | | |
| 09 Flujos de negocio / verificaciones de estado | | | | |

## Defectos encontrados

| ID defecto | Caso | Precondiciones | Pasos | HTTP esperado | HTTP observado | Severidad |
|---|---|---|---|---|---|---|
| | | | | | | |
