# Registro de ejecución manual — PulsePass E2E

| Campo | Valor |
|---|---|
| Ejecutado por | Camilo Hincapie y Cesar Acosta |
| Fecha | 18/10/2026 (pasos 001–119) y 19/10/2026 (pasos 120–125) |
| runId | r01 |
| Rama probada | feature/pruebaE2E |
| Base de datos | pulsepass_e2e |

Resultado: `PASS` / `FAIL` / `BLOCKED` / `NOT RUN`. No pegar tokens, contraseñas ni datos personales reales.

Los 125 pasos quedaron en `PASS`. Los pasos 093, 095 y 096 fallaron en su primer intento (defecto D-01) y pasaron al repetirlos tras la corrección. La columna Fecha/hora registra solo la fecha.

| # | Caso | Actor | Ruta | HTTP esperado | HTTP observado | JSON / estado observado | Resultado | Fecha/hora | Observación |
|---|---|---|---|---|---|---|---|---|---|
| 001 | PRE-01 | ANONYMOUS | `GET /api/venues/active` | 200 | 200 | El arreglo contiene VEN-E2E-01 y VEN-E2E-MINI | PASS | 18/10/2026 | Sin observaciones |
| 002 | TC-BIZ-001a | ANONYMOUS | `GET /api/venues/active` | 200 | 200 | Incluye VEN-E2E-01 y VEN-E2E-MINI con active=true. | PASS | 18/10/2026 | Sin observaciones |
| 003 | TC-BIZ-001b | ANONYMOUS | `GET /api/venues/{{venueActiveCode}}` | 200 | 200 | code = VEN-E2E-01, capacity = 500, active = true. | PASS | 18/10/2026 | Sin observaciones |
| 004 | TC-HTTP-VEN-404 | ANONYMOUS | `GET /api/venues/NO-EXISTE-{{runId}}` | 404 | 404 | Not Found | PASS | 18/10/2026 | Sin observaciones |
| 005 | TC-BIZ-002a | ANONYMOUS | `GET /api/artists/active` | 200 | 200 | Lista ordenada por stageName con los 5 artistas de la migración V2 (Caribbean Sound, Digital Pulse, Neon Waves, Ocean Drive, Solar Beat). | PASS | 18/10/2026 | Sin observaciones |
| 006 | TC-BIZ-002b | ANONYMOUS | `GET /api/artists/{{artistId}}` | 200 | 200 | stageName = Solar Beat, genre = Electronic, active = true. | PASS | 18/10/2026 | Sin observaciones |
| 007 | TC-HTTP-ART-404 | ANONYMOUS | `GET /api/artists/999999` | 404 | 404 | ErrorResponse con 5 campos (timestamp, status, error, message, details); status igual al HTTP; sin stack trace, hash, token ni secretos. | PASS | 18/10/2026 | Sin observaciones |
| 008 | TC-HTTP-ART-400 | ANONYMOUS | `GET /api/artists/abc` | 400 | 400 | message "Invalid request parameter", details.id = "Invalid value: abc". | PASS | 18/10/2026 | Sin observaciones |
| 009 | TC-BIZ-002c | ANONYMOUS | `GET /api/artists/by-stage-name?stageName={{artistStageName}}` | 200 | 200 | Mismo id que en TC-BIZ-002a (consulta consistente). | PASS | 18/10/2026 | Sin observaciones |
| 010 | TC-HTTP-ART-404b | ANONYMOUS | `GET /api/artists/by-stage-name?stageName=Nadie {{runId}}` | 404 | 404 | message "Artist not found: Nadie {{runId}}". | PASS | 18/10/2026 | Sin observaciones |
| 011 | TC-AUTH-001 | ANONYMOUS | `POST /api/users` | 201 | 201 | username/email iguales a los enviados, active = true. | PASS | 18/10/2026 | Sin observaciones |
| 012 | TC-BIZ-006 | ANONYMOUS | `POST /api/users` | 201 | 201 | Identidad distinta a USER_A (id, username y email diferentes). | PASS | 18/10/2026 | Sin observaciones |
| 013 | TC-SEC-016 | ANONYMOUS | `POST /api/users` | 201 | 201 | 201: el campo role se ignora (no existe en RegisterUserRequest). Un 400 también es aceptable: lo importante es que NO obtenga ADMIN. | PASS | 18/10/2026 | Sin observaciones |
| 014 | TC-BIZ-006b | ANONYMOUS | `POST /api/users` | 409 | 409 | status 409, error "Conflict", message "Username already exists: …". | PASS | 18/10/2026 | Sin observaciones |
| 015 | TC-HTTP-USR-400 | ANONYMOUS | `POST /api/users` | 400 | 400 | details.email = "Email format is invalid" y details.password = "Password must have between 12 and 72 characters". | PASS | 18/10/2026 | Sin observaciones |
| 016 | TC-HTTP-JSON-400 | ANONYMOUS | `POST /api/users` | 400 | 400 | message "Malformed or invalid JSON request", details.body presente. | PASS | 18/10/2026 | Sin observaciones |
| 017 | TC-AUTH-002 | ANONYMOUS | `POST /api/auth/login` | 200 | 200 | tokenType = "Bearer", expiresIn = {{expectedTokenTtlSeconds}} (900), accessToken no vacío. | PASS | 18/10/2026 | Sin observaciones |
| 018 | TC-AUTH-003 | ANONYMOUS | `POST /api/auth/login` | 200 | 200 | 200 con un token nuevo | PASS | 18/10/2026 | Sin observaciones |
| 019 | TC-AUTH-LOGIN-B | ANONYMOUS | `POST /api/auth/login` | 200 | 200 | tokenType Bearer, expiresIn 900. | PASS | 18/10/2026 | Sin observaciones |
| 020 | TC-AUTH-LOGIN-C | ANONYMOUS | `POST /api/auth/login` | 200 | 200 | tokenType Bearer, expiresIn 900. | PASS | 18/10/2026 | Sin observaciones |
| 021 | TC-AUTH-LOGIN-ADMIN | ANONYMOUS | `POST /api/auth/login` | 200 | 200 | Creó el token ADMIN | PASS | 18/10/2026 | Sin observaciones |
| 022 | TC-AUTH-LOGIN-STAFF | ANONYMOUS | `POST /api/auth/login` | 200 | 200 | Creó el token STAFF | PASS | 18/10/2026 | Sin observaciones |
| 023 | TC-AUTH-004 | ANONYMOUS | `POST /api/auth/login` | 401 | 401 | "message": "Credenciales invalidas o autenticacion requerida" | PASS | 18/10/2026 | Sin observaciones |
| 024 | TC-AUTH-005 | ANONYMOUS | `POST /api/auth/login` | 401 | 401 | Mismo status y MISMO message que TC-AUTH-004: no se puede saber si el usuario existe. | PASS | 18/10/2026 | Sin observaciones |
| 025 | TC-HTTP-LOGIN-400 | ANONYMOUS | `POST /api/auth/login` | 400 | 400 | details.identifier = "Identifier is required" y details.password = "Password is required". | PASS | 18/10/2026 | Sin observaciones |
| 026 | TC-AUTH-006 | USER_A | `GET /api/auth/me` | 200 | 200 | Sin password, hash ni token en el cuerpo. | PASS | 18/10/2026 | Sin observaciones |
| 027 | TC-AUTH-ME-B | USER_B | `GET /api/auth/me` | 200 | 200 | username = {{userBUsername}}, roles = ["USER"]; id distinto al de USER_A. | PASS | 18/10/2026 | Sin observaciones |
| 028 | TC-SEC-016b | USER_C | `GET /api/auth/me` | 200 | 200 | roles = ["USER"] exactamente; no aparece ADMIN. | PASS | 18/10/2026 | Comprobación previa a la desactivación de USER_C (ver paso 120). |
| 029 | TC-AUTH-ME-ADMIN | ADMIN | `GET /api/auth/me` | 200 | 200 | username = {{adminIdentifier}}, roles = ["ADMIN"]. | PASS | 18/10/2026 | Sin observaciones |
| 030 | TC-AUTH-ME-STAFF | STAFF | `GET /api/auth/me` | 200 | 200 | username = {{staffIdentifier}}, roles = ["STAFF"]. | PASS | 18/10/2026 | Sin observaciones |
| 031 | TC-AUTH-007 | ANONYMOUS | `GET /api/auth/me` | 401 | 401 | status 401, error "Unauthorized", "message": "Credenciales invalidas o autenticacion requerida". | PASS | 18/10/2026 | Sin observaciones |
| 032 | TC-AUTH-008 | ANONYMOUS | `GET /api/auth/me` | 401 | 401 | 401 + ErrorResponse; nunca 500. | PASS | 18/10/2026 | Sin observaciones |
| 033 | TC-AUTH-009 | ANONYMOUS | `GET /api/auth/me` | 401 | 401 | 401: la firma ya no coincide, no entra al controller. | PASS | 18/10/2026 | Sin observaciones |
| 034 | TC-AUTH-011 | ANONYMOUS | `GET /api/auth/me` | 401 | 401 | 401 esperado | PASS | 18/10/2026 | Sin observaciones |
| 035 | TC-AUTH-011b | ANONYMOUS | `GET /api/auth/me` | 401 | 401 | 401: los tokens sin firma se rechazan siempre. | PASS | 18/10/2026 | Sin observaciones |
| 036 | TC-AUTH-012 | ANONYMOUS | `GET /api/auth/me?access_token={{userAToken}}` | 401 | 401 | 401: el token en la URL se ignora (solo vale el header Authorization). | PASS | 18/10/2026 | Sin observaciones |
| 037 | TC-AUTH-015 | ANONYMOUS | `GET /api/events/published` | 401 | 401 | Si llega un Bearer inválido se responde 401 aunque la ruta sea pública; nunca se autentica por accidente. | PASS | 18/10/2026 | Sin observaciones |
| 038 | TC-SEC-001a | ANONYMOUS | `POST /api/events` | 401 | 401 | El evento NO se crea | PASS | 18/10/2026 | Sin observaciones |
| 039 | TC-SEC-001b | USER_A | `POST /api/events` | 403 | 403 | status 403, error "Forbidden", "message": "No tiene permisos para realizar esta operacion". | PASS | 18/10/2026 | Sin observaciones |
| 040 | TC-SEC-001c | STAFF | `POST /api/events` | 403 | 403 | 403: STAFF no administra el catálogo. | PASS | 18/10/2026 | Sin observaciones |
| 041 | TC-BIZ-003 | ADMIN | `POST /api/events` | 201 | 201 | eventCode = {{publishedEventCode}}, status = "DRAFT", venueCode = VEN-E2E-01, artists = []. | PASS | 18/10/2026 | Sin observaciones |
| 042 | TC-BIZ-003b | ADMIN | `POST /api/events` | 201 | 201 | status = "DRAFT". Este evento NUNCA se publica. | PASS | 18/10/2026 | Sin observaciones |
| 043 | TC-BIZ-003c | ADMIN | `POST /api/events` | 201 | 201 | venueCode = VEN-E2E-MINI, status DRAFT. Servirá para probar aforo agotado. | PASS | 18/10/2026 | Sin observaciones |
| 044 | TC-BIZ-003d | ADMIN | `POST /api/events` | 201 | 201 | minimumAge = 99. Servirá para probar edad insuficiente. | PASS | 18/10/2026 | Sin observaciones |
| 045 | TC-BIZ-015a | ADMIN | `POST /api/events` | 409 | 409 | message "Event code already exists: …". | PASS | 18/10/2026 | Sin observaciones |
| 046 | TC-BIZ-015b | ADMIN | `POST /api/events` | 409 | 409 | message "Venue is not active: VEN-E2E-OFF". | PASS | 18/10/2026 | Sin observaciones |
| 047 | TC-BIZ-015c | ADMIN | `POST /api/events` | 404 | 404 | message "Venue not found: NO-EXISTE". | PASS | 18/10/2026 | Sin observaciones |
| 048 | TC-BIZ-015d | ADMIN | `POST /api/events` | 409 | 409 | message "Event date must be in the future.". | PASS | 18/10/2026 | Sin observaciones |
| 049 | TC-BIZ-015e | ADMIN | `POST /api/events` | 400 | 400 | message "Malformed or invalid JSON request" (OPERA no existe en EventCategory). | PASS | 18/10/2026 | Sin observaciones |
| 050 | TC-BIZ-015f | ADMIN | `POST /api/events` | 400 | 400 | message "Validation failed"; details incluye eventCode, name, category, eventDate, minimumAge y venueCode. | PASS | 18/10/2026 | Sin observaciones |
| 051 | TC-SEC-015 | ADMIN | `GET /api/events/{{draftEventCode}}` | 200 | 200 | status = "DRAFT" y datos completos del evento. | PASS | 18/10/2026 | Sin observaciones |
| 052 | TC-SEC-015b | STAFF | `GET /api/events/{{draftEventCode}}` | 200 | 200 | 200: STAFF/ADMIN sí pueden ver borradores. | PASS | 18/10/2026 | Sin observaciones |
| 053 | TC-SEC-004 | ANONYMOUS | `GET /api/events/{{draftEventCode}}` | 403 | 403 | 403 (política fija para recursos no visibles: expectedForeignResourceStatus). | PASS | 18/10/2026 | Sin observaciones |
| 054 | TC-SEC-004b | USER_A | `GET /api/events/{{draftEventCode}}` | 403 | 403 | 403 sin datos del borrador. | PASS | 18/10/2026 | Sin observaciones |
| 055 | TC-SEC-003 | USER_A | `POST /api/events/{{publishedEventCode}}/artists/{{artistId}}` | 403 | 403 | Responde que no tiene permisos. | PASS | 18/10/2026 | Sin observaciones |
| 056 | TC-BIZ-005 | ADMIN | `POST /api/events/{{publishedEventCode}}/artists/{{artistId}}` | 200 | 200 | artists contiene exactamente 1 elemento con id = {{artistId}} y stageName Solar Beat. | PASS | 18/10/2026 | Sin observaciones |
| 057 | TC-BIZ-005b | ADMIN | `POST /api/events/{{draftEventCode}}/artists/{{artistId}}` | 200 | 200 | Se asocia correctamente. | PASS | 18/10/2026 | Sin observaciones |
| 058 | TC-BIZ-005c | ADMIN | `POST /api/events/{{publishedEventCode}}/artists/{{artistId}}` | 409 | 409 | message "Artist Solar Beat is already associated with event …". | PASS | 18/10/2026 | Sin observaciones |
| 059 | TC-BIZ-005d | ADMIN | `POST /api/events/{{publishedEventCode}}/artists/999999` | 404 | 404 | message "Artist not found: 999999". | PASS | 18/10/2026 | Sin observaciones |
| 060 | TC-BIZ-004a | ANONYMOUS | `GET /api/events/published` | 200 | 200 | Ningún elemento tiene eventCode {{publishedEventCode}}, {{draftEventCode}}, {{miniEventCode}} ni {{ageEventCode}} (todos en DRAFT). | PASS | 18/10/2026 | Sin observaciones |
| 061 | TC-SEC-002a | USER_A | `PATCH /api/events/{{publishedEventCode}}/publish` | 403 | 403 | 403 + ErrorResponse. | PASS | 18/10/2026 | Sin observaciones |
| 062 | TC-SEC-002b | STAFF | `PATCH /api/events/{{publishedEventCode}}/publish` | 403 | 403 | 403 + ErrorResponse. | PASS | 18/10/2026 | Sin observaciones |
| 063 | TC-BIZ-004 | ADMIN | `PATCH /api/events/{{publishedEventCode}}/publish` | 200 | 200 | status = "PUBLISHED" y conserva el artista asociado. | PASS | 18/10/2026 | Sin observaciones |
| 064 | TC-BIZ-004b | ADMIN | `PATCH /api/events/{{publishedEventCode}}/publish` | 409 | 409 | message "Only DRAFT events can be published. Current status: PUBLISHED". | PASS | 18/10/2026 | Sin observaciones |
| 065 | TC-BIZ-004c | ADMIN | `PATCH /api/events/{{miniEventCode}}/publish` | 200 | 200 | status = "PUBLISHED". | PASS | 18/10/2026 | Sin observaciones |
| 066 | TC-BIZ-004d | ADMIN | `PATCH /api/events/{{ageEventCode}}/publish` | 200 | 200 | status = "PUBLISHED". | PASS | 18/10/2026 | Sin observaciones |
| 067 | TC-BIZ-004e | ANONYMOUS | `GET /api/events/{{publishedEventCode}}` | 200 | 200 | 200 sin token: status PUBLISHED, artists con Solar Beat. | PASS | 18/10/2026 | Sin observaciones |
| 068 | TC-BIZ-004f | ANONYMOUS | `GET /api/events/published` | 200 | 200 | Incluye {{publishedEventCode}}, {{miniEventCode}} y {{ageEventCode}}. | PASS | 18/10/2026 | Sin observaciones |
| 069 | TC-SEC-005 | ANONYMOUS | `GET /api/events/by-artist?stageName={{artistStageName}}` | 200 | 200 | Incluye {{publishedEventCode}}. | PASS | 18/10/2026 | Sin observaciones |
| 070 | TC-BIZ-005e | ADMIN | `GET /api/events/by-artist?stageName={{artistStageName}}` | 200 | 200 | El operador autorizado ve todos los estados. | PASS | 18/10/2026 | Sin observaciones |
| 071 | TC-HTTP-EVT-404 | ANONYMOUS | `GET /api/events/NO-EXISTE-{{runId}}` | 404 | 404 | message "Event not found: …". | PASS | 18/10/2026 | Sin observaciones |
| 072 | TC-HTTP-EVT-404b | ANONYMOUS | `GET /api/events/by-artist?stageName=Nadie` | 404 | 404 | message "Artist not found: Nadie". | PASS | 18/10/2026 | Sin observaciones |
| 073 | TC-SEC-008 | USER_A | `POST /api/tickets` | 403 | 403 | No se crea ningún ticket (se confirma en TC-SEC-008b). | PASS | 18/10/2026 | Sin observaciones |
| 074 | TC-SEC-008c | ANONYMOUS | `POST /api/tickets` | 401 | 401 | 401 + ErrorResponse. | PASS | 18/10/2026 | Sin observaciones |
| 075 | TC-SEC-008d | STAFF | `POST /api/tickets` | 403 | 403 | 403: comprar es solo para USER. | PASS | 18/10/2026 | Sin observaciones |
| 076 | TC-BIZ-007 | USER_A | `POST /api/tickets` | 201 | 201 | status = "PAID", type = GENERAL, price = 100000.00, userEmail = {{userAEmail}}, eventCode = {{publishedEventCode}}. | PASS | 18/10/2026 | Sin observaciones |
| 077 | TC-BIZ-007b | USER_A | `POST /api/tickets` | 201 | 201 | status PAID, type VIP, price = 200000.00. | PASS | 18/10/2026 | Sin observaciones |
| 078 | TC-BIZ-007c | USER_B | `POST /api/tickets` | 201 | 201 | status PAID, userEmail = {{userBEmail}}. | PASS | 18/10/2026 | Sin observaciones |
| 079 | TC-HTTP-TKT-400 | USER_A | `POST /api/tickets` | 400 | 400 | message "Malformed or invalid JSON request" (PLATINUM no existe en TicketType). | PASS | 18/10/2026 | Sin observaciones |
| 080 | TC-HTTP-TKT-400b | USER_A | `POST /api/tickets` | 400 | 400 | details: userEmail, eventCode y type requeridos. | PASS | 18/10/2026 | Sin observaciones |
| 081 | TC-BIZ-014a | USER_A | `POST /api/tickets` | 409 | 409 | message "Tickets can only be purchased for PUBLISHED events. Current status: DRAFT". | PASS | 18/10/2026 | Sin observaciones |
| 082 | TC-BIZ-014b | USER_A | `POST /api/tickets` | 409 | 409 | message "User does not meet minimum age.". | PASS | 18/10/2026 | Sin observaciones |
| 083 | TC-BIZ-014c | USER_A | `POST /api/tickets` | 404 | 404 | message "Event not found: …". | PASS | 18/10/2026 | Sin observaciones |
| 084 | TC-BIZ-014d | USER_A | `POST /api/tickets` | 201 | 201 | status PAID. Con esta compra el evento queda SOLD_OUT. | PASS | 18/10/2026 | Sin observaciones |
| 085 | TC-BIZ-014e | USER_B | `POST /api/tickets` | 409 | 409 | message "… Current status: SOLD_OUT". No se crea ticket. | PASS | 18/10/2026 | Sin observaciones |
| 086 | TC-BIZ-014f | ADMIN | `GET /api/events/{{miniEventCode}}` | 200 | 200 | status = "SOLD_OUT" (estado persistido). | PASS | 18/10/2026 | Sin observaciones |
| 087 | TC-BIZ-008 | USER_A | `GET /api/tickets/{{ticketCancelableCode}}` | 200 | 200 (1.er intento: 500) | ticketCode = T1, status PAID, userEmail = {{userAEmail}}. | PASS | 18/10/2026 | Primer intento: 500, con la variable ticketCancelableCode vacía (aún no se había copiado el ticketCode de T1). Con la variable cargada: 200. |
| 088 | TC-SEC-009 | USER_B | `GET /api/tickets/{{ticketCancelableCode}}` | 403 | 403 | El cuerpo NO contiene datos del ticket. | PASS | 18/10/2026 | Sin observaciones |
| 089 | TC-BIZ-008b | STAFF | `GET /api/tickets/{{ticketCancelableCode}}` | 200 | 200 | STAFF puede consultar tickets (limitación MVP: STAFF global). | PASS | 18/10/2026 | Sin observaciones |
| 090 | TC-SEC-009b | ANONYMOUS | `GET /api/tickets/{{ticketCancelableCode}}` | 401 | 401 | ErrorResponse. | PASS | 18/10/2026 | Sin observaciones |
| 091 | TC-SEC-009c | USER_A | `GET /api/tickets/TCK-NOEXISTE` | 403 | 403 | 403 (no 404): para un USER, un código que no es suyo y uno que no existe se ven igual, así que no se pueden enumerar tickets. | PASS | 18/10/2026 | Sin observaciones |
| 092 | TC-HTTP-TKT-404 | STAFF | `GET /api/tickets/TCK-NOEXISTE` | 404 | 404 | 404 "Ticket not found: TCK-NOEXISTE" (STAFF sí está autorizado, así que ve el 404 real). | PASS | 18/10/2026 | Sin observaciones |
| 093 | TC-BIZ-008c | USER_A | `GET /api/tickets/by-user?email={{userAEmail}}` | 200 | 200 (1.er intento: 500) | Contiene T1, T2 y el ticket del evento de aforo 1 (3 en total), todos con userEmail {{userAEmail}}. | PASS | 18/10/2026 | Primer intento: 500 (ver D-01). Repetido tras corregir TicketRepository: 200. |
| 094 | TC-SEC-010 | USER_B | `GET /api/tickets/by-user?email={{userAEmail}}` | 403 | 403 | 403 sin información. | PASS | 18/10/2026 | Sin observaciones |
| 095 | TC-BIZ-008d | ADMIN | `GET /api/tickets/by-user?email={{userAEmail}}` | 200 | 200 (1.er intento: 500) | Lista de tickets de USER_A, todos con su userEmail. ADMIN autorizado. | PASS | 18/10/2026 | Primer intento: 500 (ver D-01). Repetido tras corregir TicketRepository: 200. |
| 096 | TC-SEC-008b | USER_B | `GET /api/tickets/by-user?email={{userBEmail}}` | 200 | 200 (1.er intento: 500) | Exactamente 1 ticket: el STUDENT que USER_B compró. Ni la compra ajena de TC-SEC-008 ni la de aforo agotado crearon tickets para B. | PASS | 18/10/2026 | Primer intento: 500 (ver D-01). Repetido tras corregir TicketRepository: 200. |
| 097 | TC-BIZ-USR-02 | USER_A | `GET /api/users/by-email?email={{userAEmail}}` | 200 | 200 | UserResponse de USER_A; sin password ni hash. | PASS | 18/10/2026 | Sin observaciones |
| 098 | TC-SEC-006 | USER_B | `GET /api/users/by-email?email={{userAEmail}}` | 403 | 403 | 403 sin información personal. | PASS | 18/10/2026 | Sin observaciones |
| 099 | TC-BIZ-USR-02b | ADMIN | `GET /api/users/by-email?email={{userAEmail}}` | 200 | 200 | ADMIN autorizado. | PASS | 18/10/2026 | Sin observaciones |
| 100 | TC-HTTP-USR-400b | USER_A | `GET /api/users/by-email` | 400 | 400 | message "Missing request parameter", details.email = "Parameter is required". | PASS | 18/10/2026 | Sin observaciones |
| 101 | TC-BIZ-USR-03 | USER_A | `GET /api/users/by-username?username={{userAUsername}}` | 200 | 200 | UserResponse de USER_A. | PASS | 18/10/2026 | Sin observaciones |
| 102 | TC-SEC-007 | USER_B | `GET /api/users/by-username?username={{userAUsername}}` | 403 | 403 | 403 sin información personal. | PASS | 18/10/2026 | Sin observaciones |
| 103 | TC-SEC-007b | ANONYMOUS | `GET /api/users/by-username?username={{userAUsername}}` | 401 | 401 | 401 + ErrorResponse. | PASS | 18/10/2026 | Sin observaciones |
| 104 | TC-SEC-011 | USER_B | `PATCH /api/tickets/{{ticketCancelableCode}}/cancel` | 403 | 403 | 403 + ErrorResponse. | PASS | 18/10/2026 | Sin observaciones |
| 105 | TC-SEC-012 | USER_B | `PATCH /api/tickets/{{ticketUsableCode}}/use` | 403 | 403 | 403 + ErrorResponse. | PASS | 18/10/2026 | Sin observaciones |
| 106 | TC-SEC-012b | USER_A | `PATCH /api/tickets/{{ticketUsableCode}}/use` | 403 | 403 | Marcar uso es solo para STAFF/ADMIN. | PASS | 18/10/2026 | Sin observaciones |
| 107 | TC-SEC-011b | USER_A | `GET /api/tickets/by-user?email={{userAEmail}}` | 200 | 200 | T1 y T2 con status PAID: las 3 denegaciones anteriores no mutaron nada. | PASS | 18/10/2026 | Sin observaciones |
| 108 | TC-SEC-013 | USER_A | `GET /api/events/{{publishedEventCode}}/tickets/paid` | 403 | 403 | 403 + ErrorResponse. | PASS | 18/10/2026 | Sin observaciones |
| 109 | TC-SEC-013b | ANONYMOUS | `GET /api/events/{{publishedEventCode}}/tickets/paid` | 401 | 401 | 401 + ErrorResponse. | PASS | 18/10/2026 | Sin observaciones |
| 110 | TC-BIZ-009 | STAFF | `GET /api/events/{{publishedEventCode}}/tickets/paid` | 200 | 200 | Contiene T1, T2 y el ticket de USER_B (3), todos PAID. | PASS | 18/10/2026 | Sin observaciones |
| 111 | TC-BIZ-010 | USER_A | `PATCH /api/tickets/{{ticketCancelableCode}}/cancel` | 200 | 200 | status = "CANCELLED". | PASS | 18/10/2026 | Sin observaciones |
| 112 | TC-BIZ-010b | USER_A | `GET /api/tickets/{{ticketCancelableCode}}` | 200 | 200 | status = "CANCELLED" persistido. | PASS | 18/10/2026 | Sin observaciones |
| 113 | TC-BIZ-011 | USER_A | `PATCH /api/tickets/{{ticketCancelableCode}}/cancel` | 409 | 409 | message "Only PAID tickets can be cancelled. Current status: CANCELLED". | PASS | 18/10/2026 | Sin observaciones |
| 114 | TC-BIZ-009b | STAFF | `GET /api/events/{{publishedEventCode}}/tickets/paid` | 200 | 200 | Solo T2 y el ticket de USER_B. T1 (CANCELLED) no aparece. | PASS | 18/10/2026 | Sin observaciones |
| 115 | TC-SEC-014 | STAFF | `PATCH /api/tickets/{{ticketUsableCode}}/use` | 200 | 200 | status = "USED". | PASS | 18/10/2026 | Sin observaciones |
| 116 | TC-BIZ-012 | USER_A | `GET /api/tickets/{{ticketUsableCode}}` | 200 | 200 | status = "USED" persistido. | PASS | 18/10/2026 | Sin observaciones |
| 117 | TC-BIZ-013 | STAFF | `PATCH /api/tickets/{{ticketUsableCode}}/use` | 409 | 409 | message "Only PAID tickets can be marked as used. Current status: USED". | PASS | 18/10/2026 | Sin observaciones |
| 118 | TC-SEC-011c | STAFF | `PATCH /api/tickets/{{ticketUserBCode}}/cancel` | 403 | 403 | Cancelar es del propietario o ADMIN. | PASS | 18/10/2026 | Sin observaciones |
| 119 | TC-BIZ-010c | ADMIN | `PATCH /api/tickets/{{ticketUserBCode}}/cancel` | 200 | 200 | status CANCELLED: ADMIN expresamente autorizado. | PASS | 18/10/2026 | Sin observaciones |
| 120 | TC-AUTH-013 | USER_C | `GET /api/auth/me` | 401 | 401 | El filtro revalida el estado en cada request (SEC-011). | PASS | 19/10/2026 | Antes de ejecutar el SQL de desactivación devolvía 200 (equivale a TC-SEC-016b). Tras `UPDATE users SET active = false WHERE username = 'userc_r01'`: 401. |
| 121 | TC-AUTH-013b | ANONYMOUS | `POST /api/auth/login` | 401 | 401 | 401 con el mismo mensaje genérico (no revela que está inactivo). | PASS | 19/10/2026 | Ejecutada con USER_C ya desactivado. |
| 122 | TC-SEC-017 | STAFF | `GET /api/events/{{publishedEventCode}}/tickets/paid` | 403 | 403 | El rol se recarga desde la BD en cada request; el token viejo ya no sirve para operaciones STAFF. | PASS | 19/10/2026 | Antes de revocar el rol devolvía 200. Tras eliminar el rol STAFF de `laura` en `user_roles`: 403. La restauración del rol con el INSERT de la guía debe confirmarse. |
| 123 | TC-SEC-018 | ADMIN | `DELETE /api/events/{{publishedEventCode}}` | 403 | 403 | denyAll() para rutas no definidas, incluso para ADMIN. El evento sigue existiendo. | PASS | 19/10/2026 | Sin observaciones |
| 124 | TC-SEC-018b | ANONYMOUS | `DELETE /api/events/{{publishedEventCode}}` | 401 | 401 | 401 + ErrorResponse. | PASS | 19/10/2026 | Sin observaciones |
| 125 | TC-AUTH-010 | ANONYMOUS | `GET /api/auth/me` | 401 | 401 | 401 + ErrorResponse. | PASS | 19/10/2026 | Sin observaciones |

## Consolidación

Resultado final de cada paso, después de repetir los que fallaron en el primer intento.

| Módulo | PASS | FAIL | BLOCKED | NOT RUN |
|---|---|---|---|---|
| 00 Preparación / conectividad | 1 | 0 | 0 | 0 |
| 01 Venues (2 rutas) | 2 | 0 | 0 | 0 |
| 02 Artists (3 rutas) | 3 | 0 | 0 | 0 |
| 03 Auth / usuarios (registro, login, me) | 14 | 0 | 0 | 0 |
| 04 Usuarios / consulta y propiedad (2 rutas) | 3 | 0 | 0 | 0 |
| 05 Events / administración y visibilidad (6 rutas) | 14 | 0 | 0 | 0 |
| 06 Tickets / compra, consulta y ciclo de vida (6 rutas) | 12 | 0 | 0 | 0 |
| 07 Seguridad / escenarios negativos | 40 | 0 | 0 | 0 |
| 08 Validaciones y errores HTTP | 29 | 0 | 0 | 0 |
| 09 Flujos de negocio / verificaciones de estado | 7 | 0 | 0 | 0 |
| **Total** | **125** | **0** | **0** | **0** |

## Defectos encontrados

| ID defecto | Caso | Precondiciones | Pasos | HTTP esperado | HTTP observado | Severidad |
|---|---|---|---|---|---|---|
| D-01 | TC-BIZ-008c, TC-BIZ-008d y TC-SEC-008b (pasos 093, 095 y 096) | Tickets de USER_A (T1, T2 y el de aforo 1) y de USER_B ya comprados; token vigente del actor | `GET /api/tickets/by-user?email=...` como USER_A, ADMIN y USER_B | 200 | 500, `message` "An unexpected error occurred", `details` vacío | Alta |

### D-01: detalle

- **Causa:** el método `findByUserEmailIgnoreCaseOrderByPurchaseDateDesc` de `TicketRepository` lanzaba `IllegalArgumentException: Unable to ignore case of com.pulsepass.domain.User types, the property 'user' must reference a String`. Spring Data intentaba aplicar `IgnoreCase` sobre la relación `user` y no sobre un `String`, así que la consulta fallaba en cada ejecución, con cualquier actor.
- **Corrección:** se reemplazó la derivación del nombre del método por una consulta explícita con el mismo nombre de método:
  `@Query("select t from Ticket t where lower(t.user.email) = lower(:email) order by t.purchaseDate desc")`
- **Verificación:** al repetir los pasos 093, 095 y 096 con la corrección, los tres respondieron 200.
- **Estado:** corregido y verificado.

## Cambios en el código durante la ejecución

| Archivo | Cambio | Motivo |
|---|---|---|
| `repository/TicketRepository.java` | `findByUserEmailIgnoreCaseOrderByPurchaseDateDesc` pasó a usar `@Query` | Corrección de D-01 |
| `exception/GlobalExceptionHandler.java` | Se agregó `log.error("Error inesperado", ex);` en `handleUnexpected` | El manejador devolvía el 500 sin registrar la causa; el log permitió diagnosticar D-01 |

## Incidencias de ejecución (no son defectos)

| Paso | Qué pasó | Resolución |
|---|---|---|
| 087 | 500 en el primer intento porque `ticketCancelableCode` estaba vacía | Se cargó el `ticketCode` de T1 en la variable y se repitió: 200 |
| 120 | 200 porque aún no se había ejecutado el SQL de desactivación de USER_C | Se ejecutó el `UPDATE` sobre `pulsepass_e2e` y se repitió: 401 |
| 122 | 200 porque aún no se había revocado el rol STAFF de `laura` | Se ejecutó el `DELETE` sobre `user_roles` y se repitió: 403 |
