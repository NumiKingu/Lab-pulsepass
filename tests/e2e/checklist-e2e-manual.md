# Checklist E2E manual — orden de ejecución

Marca cada paso al enviarlo y anota el resultado en `registro-ejecucion-manual.md`.

## Antes de empezar (DoR)

- [ ] PostgreSQL arriba y base `pulsepass_e2e` creada
- [ ] App arrancada con perfil `dev`, BD `pulsepass_e2e`, secreto JWT exclusivo de E2E y contraseñas de seed
- [ ] Log de arranque muestra `Cuenta ADMIN creada…` y `Cuenta STAFF creada…` (o ya existían)
- [ ] `seed-e2e.sql` ejecutado sin errores
- [ ] Colección y ambiente importados; ambiente `PulsePass-local (E2E)` seleccionado
- [ ] Contraseñas escritas en **Current value** (no en Initial value)
- [ ] `runId` nuevo si ya ejecutaste antes sobre la misma base


## Preparación

- [ ] **001** `PRE-01` — ANONYMOUS — `GET /api/venues/active` → **200**

## Flujo A — Descubrimiento público (sin token)

- [ ] **002** `TC-BIZ-001a` — ANONYMOUS — `GET /api/venues/active` → **200**
- [ ] **003** `TC-BIZ-001b` — ANONYMOUS — `GET /api/venues/{{venueActiveCode}}` → **200**
- [ ] **004** `TC-HTTP-VEN-404` — ANONYMOUS — `GET /api/venues/NO-EXISTE-{{runId}}` → **404**
- [ ] **005** `TC-BIZ-002a` — ANONYMOUS — `GET /api/artists/active` → **200** · **copiar:** El valor de "id" del artista Solar Beat → variable artistId. No supongas que es 1.
- [ ] **006** `TC-BIZ-002b` — ANONYMOUS — `GET /api/artists/{{artistId}}` → **200**
- [ ] **007** `TC-HTTP-ART-404` — ANONYMOUS — `GET /api/artists/999999` → **404**
- [ ] **008** `TC-HTTP-ART-400` — ANONYMOUS — `GET /api/artists/abc` → **400**
- [ ] **009** `TC-BIZ-002c` — ANONYMOUS — `GET /api/artists/by-stage-name?stageName={{artistStageName}}` → **200**
- [ ] **010** `TC-HTTP-ART-404b` — ANONYMOUS — `GET /api/artists/by-stage-name?stageName=Nadie {{runId}}` → **404**

## Flujo B — Registro, login y JWT

- [ ] **011** `TC-AUTH-001` — ANONYMOUS — `POST /api/users` → **201**
- [ ] **012** `TC-BIZ-006` — ANONYMOUS — `POST /api/users` → **201**
- [ ] **013** `TC-SEC-016` — ANONYMOUS — `POST /api/users` → **201**
- [ ] **014** `TC-BIZ-006b` — ANONYMOUS — `POST /api/users` → **409**
- [ ] **015** `TC-HTTP-USR-400` — ANONYMOUS — `POST /api/users` → **400**
- [ ] **016** `TC-HTTP-JSON-400` — ANONYMOUS — `POST /api/users` → **400**
- [ ] **017** `TC-AUTH-002` — ANONYMOUS — `POST /api/auth/login` → **200** · **copiar:** accessToken → userAToken. Copia el MISMO valor también en expiredToken (lo usarás al final, cuando pasen más de 15 min).
- [ ] **018** `TC-AUTH-003` — ANONYMOUS — `POST /api/auth/login` → **200**
- [ ] **019** `TC-AUTH-LOGIN-B` — ANONYMOUS — `POST /api/auth/login` → **200** · **copiar:** accessToken → userBToken.
- [ ] **020** `TC-AUTH-LOGIN-C` — ANONYMOUS — `POST /api/auth/login` → **200** · **copiar:** accessToken → userCToken.
- [ ] **021** `TC-AUTH-LOGIN-ADMIN` — ANONYMOUS — `POST /api/auth/login` → **200** · **copiar:** accessToken → adminToken.
- [ ] **022** `TC-AUTH-LOGIN-STAFF` — ANONYMOUS — `POST /api/auth/login` → **200** · **copiar:** accessToken → staffToken.
- [ ] **023** `TC-AUTH-004` — ANONYMOUS — `POST /api/auth/login` → **401**
- [ ] **024** `TC-AUTH-005` — ANONYMOUS — `POST /api/auth/login` → **401**
- [ ] **025** `TC-HTTP-LOGIN-400` — ANONYMOUS — `POST /api/auth/login` → **400**
- [ ] **026** `TC-AUTH-006` — USER_A — `GET /api/auth/me` → **200**
- [ ] **027** `TC-AUTH-ME-B` — USER_B — `GET /api/auth/me` → **200**
- [ ] **028** `TC-SEC-016b` — USER_C — `GET /api/auth/me` → **200**
- [ ] **029** `TC-AUTH-ME-ADMIN` — ADMIN — `GET /api/auth/me` → **200**
- [ ] **030** `TC-AUTH-ME-STAFF` — STAFF — `GET /api/auth/me` → **200**
- [ ] **031** `TC-AUTH-007` — ANONYMOUS — `GET /api/auth/me` → **401**
- [ ] **032** `TC-AUTH-008` — ANONYMOUS — `GET /api/auth/me` → **401**
- [ ] **033** `TC-AUTH-009` — ANONYMOUS — `GET /api/auth/me` → **401**
- [ ] **034** `TC-AUTH-011` — ANONYMOUS — `GET /api/auth/me` → **401**
- [ ] **035** `TC-AUTH-011b` — ANONYMOUS — `GET /api/auth/me` → **401**
- [ ] **036** `TC-AUTH-012` — ANONYMOUS — `GET /api/auth/me?access_token={{userAToken}}` → **401**
- [ ] **037** `TC-AUTH-015` — ANONYMOUS — `GET /api/events/published` → **401**

## Flujo C — Gestión del catálogo (ADMIN)

- [ ] **038** `TC-SEC-001a` — ANONYMOUS — `POST /api/events` → **401**
- [ ] **039** `TC-SEC-001b` — USER_A — `POST /api/events` → **403**
- [ ] **040** `TC-SEC-001c` — STAFF — `POST /api/events` → **403**
- [ ] **041** `TC-BIZ-003` — ADMIN — `POST /api/events` → **201**
- [ ] **042** `TC-BIZ-003b` — ADMIN — `POST /api/events` → **201**
- [ ] **043** `TC-BIZ-003c` — ADMIN — `POST /api/events` → **201**
- [ ] **044** `TC-BIZ-003d` — ADMIN — `POST /api/events` → **201**
- [ ] **045** `TC-BIZ-015a` — ADMIN — `POST /api/events` → **409**
- [ ] **046** `TC-BIZ-015b` — ADMIN — `POST /api/events` → **409**
- [ ] **047** `TC-BIZ-015c` — ADMIN — `POST /api/events` → **404**
- [ ] **048** `TC-BIZ-015d` — ADMIN — `POST /api/events` → **409**
- [ ] **049** `TC-BIZ-015e` — ADMIN — `POST /api/events` → **400**
- [ ] **050** `TC-BIZ-015f` — ADMIN — `POST /api/events` → **400**
- [ ] **051** `TC-SEC-015` — ADMIN — `GET /api/events/{{draftEventCode}}` → **200**
- [ ] **052** `TC-SEC-015b` — STAFF — `GET /api/events/{{draftEventCode}}` → **200**
- [ ] **053** `TC-SEC-004` — ANONYMOUS — `GET /api/events/{{draftEventCode}}` → **403**
- [ ] **054** `TC-SEC-004b` — USER_A — `GET /api/events/{{draftEventCode}}` → **403**
- [ ] **055** `TC-SEC-003` — USER_A — `POST /api/events/{{publishedEventCode}}/artists/{{artistId}}` → **403**
- [ ] **056** `TC-BIZ-005` — ADMIN — `POST /api/events/{{publishedEventCode}}/artists/{{artistId}}` → **200**
- [ ] **057** `TC-BIZ-005b` — ADMIN — `POST /api/events/{{draftEventCode}}/artists/{{artistId}}` → **200**
- [ ] **058** `TC-BIZ-005c` — ADMIN — `POST /api/events/{{publishedEventCode}}/artists/{{artistId}}` → **409**
- [ ] **059** `TC-BIZ-005d` — ADMIN — `POST /api/events/{{publishedEventCode}}/artists/999999` → **404**
- [ ] **060** `TC-BIZ-004a` — ANONYMOUS — `GET /api/events/published` → **200**
- [ ] **061** `TC-SEC-002a` — USER_A — `PATCH /api/events/{{publishedEventCode}}/publish` → **403**
- [ ] **062** `TC-SEC-002b` — STAFF — `PATCH /api/events/{{publishedEventCode}}/publish` → **403**
- [ ] **063** `TC-BIZ-004` — ADMIN — `PATCH /api/events/{{publishedEventCode}}/publish` → **200**
- [ ] **064** `TC-BIZ-004b` — ADMIN — `PATCH /api/events/{{publishedEventCode}}/publish` → **409**
- [ ] **065** `TC-BIZ-004c` — ADMIN — `PATCH /api/events/{{miniEventCode}}/publish` → **200**
- [ ] **066** `TC-BIZ-004d` — ADMIN — `PATCH /api/events/{{ageEventCode}}/publish` → **200**
- [ ] **067** `TC-BIZ-004e` — ANONYMOUS — `GET /api/events/{{publishedEventCode}}` → **200**
- [ ] **068** `TC-BIZ-004f` — ANONYMOUS — `GET /api/events/published` → **200**
- [ ] **069** `TC-SEC-005` — ANONYMOUS — `GET /api/events/by-artist?stageName={{artistStageName}}` → **200**
- [ ] **070** `TC-BIZ-005e` — ADMIN — `GET /api/events/by-artist?stageName={{artistStageName}}` → **200**
- [ ] **071** `TC-HTTP-EVT-404` — ANONYMOUS — `GET /api/events/NO-EXISTE-{{runId}}` → **404**
- [ ] **072** `TC-HTTP-EVT-404b` — ANONYMOUS — `GET /api/events/by-artist?stageName=Nadie` → **404**

## Flujo D — Compra, propiedad, cancelación y uso

- [ ] **073** `TC-SEC-008` — USER_A — `POST /api/tickets` → **403**
- [ ] **074** `TC-SEC-008c` — ANONYMOUS — `POST /api/tickets` → **401**
- [ ] **075** `TC-SEC-008d` — STAFF — `POST /api/tickets` → **403**
- [ ] **076** `TC-BIZ-007` — USER_A — `POST /api/tickets` → **201** · **copiar:** ticketCode → ticketCancelableCode (T1: se cancelará).
- [ ] **077** `TC-BIZ-007b` — USER_A — `POST /api/tickets` → **201** · **copiar:** ticketCode → ticketUsableCode (T2: se marcará como usado). Nunca uses T1 para eso.
- [ ] **078** `TC-BIZ-007c` — USER_B — `POST /api/tickets` → **201** · **copiar:** ticketCode → ticketUserBCode.
- [ ] **079** `TC-HTTP-TKT-400` — USER_A — `POST /api/tickets` → **400**
- [ ] **080** `TC-HTTP-TKT-400b` — USER_A — `POST /api/tickets` → **400**
- [ ] **081** `TC-BIZ-014a` — USER_A — `POST /api/tickets` → **409**
- [ ] **082** `TC-BIZ-014b` — USER_A — `POST /api/tickets` → **409**
- [ ] **083** `TC-BIZ-014c` — USER_A — `POST /api/tickets` → **404**
- [ ] **084** `TC-BIZ-014d` — USER_A — `POST /api/tickets` → **201**
- [ ] **085** `TC-BIZ-014e` — USER_B — `POST /api/tickets` → **409**
- [ ] **086** `TC-BIZ-014f` — ADMIN — `GET /api/events/{{miniEventCode}}` → **200**
- [ ] **087** `TC-BIZ-008` — USER_A — `GET /api/tickets/{{ticketCancelableCode}}` → **200**
- [ ] **088** `TC-SEC-009` — USER_B — `GET /api/tickets/{{ticketCancelableCode}}` → **403**
- [ ] **089** `TC-BIZ-008b` — STAFF — `GET /api/tickets/{{ticketCancelableCode}}` → **200**
- [ ] **090** `TC-SEC-009b` — ANONYMOUS — `GET /api/tickets/{{ticketCancelableCode}}` → **401**
- [ ] **091** `TC-SEC-009c` — USER_A — `GET /api/tickets/TCK-NOEXISTE` → **403**
- [ ] **092** `TC-HTTP-TKT-404` — STAFF — `GET /api/tickets/TCK-NOEXISTE` → **404**
- [ ] **093** `TC-BIZ-008c` — USER_A — `GET /api/tickets/by-user?email={{userAEmail}}` → **200**
- [ ] **094** `TC-SEC-010` — USER_B — `GET /api/tickets/by-user?email={{userAEmail}}` → **403**
- [ ] **095** `TC-BIZ-008d` — ADMIN — `GET /api/tickets/by-user?email={{userAEmail}}` → **200**
- [ ] **096** `TC-SEC-008b` — USER_B — `GET /api/tickets/by-user?email={{userBEmail}}` → **200**
- [ ] **097** `TC-BIZ-USR-02` — USER_A — `GET /api/users/by-email?email={{userAEmail}}` → **200**
- [ ] **098** `TC-SEC-006` — USER_B — `GET /api/users/by-email?email={{userAEmail}}` → **403**
- [ ] **099** `TC-BIZ-USR-02b` — ADMIN — `GET /api/users/by-email?email={{userAEmail}}` → **200**
- [ ] **100** `TC-HTTP-USR-400b` — USER_A — `GET /api/users/by-email` → **400**
- [ ] **101** `TC-BIZ-USR-03` — USER_A — `GET /api/users/by-username?username={{userAUsername}}` → **200**
- [ ] **102** `TC-SEC-007` — USER_B — `GET /api/users/by-username?username={{userAUsername}}` → **403**
- [ ] **103** `TC-SEC-007b` — ANONYMOUS — `GET /api/users/by-username?username={{userAUsername}}` → **401**
- [ ] **104** `TC-SEC-011` — USER_B — `PATCH /api/tickets/{{ticketCancelableCode}}/cancel` → **403**
- [ ] **105** `TC-SEC-012` — USER_B — `PATCH /api/tickets/{{ticketUsableCode}}/use` → **403**
- [ ] **106** `TC-SEC-012b` — USER_A — `PATCH /api/tickets/{{ticketUsableCode}}/use` → **403**
- [ ] **107** `TC-SEC-011b` — USER_A — `GET /api/tickets/by-user?email={{userAEmail}}` → **200**
- [ ] **108** `TC-SEC-013` — USER_A — `GET /api/events/{{publishedEventCode}}/tickets/paid` → **403**
- [ ] **109** `TC-SEC-013b` — ANONYMOUS — `GET /api/events/{{publishedEventCode}}/tickets/paid` → **401**
- [ ] **110** `TC-BIZ-009` — STAFF — `GET /api/events/{{publishedEventCode}}/tickets/paid` → **200**
- [ ] **111** `TC-BIZ-010` — USER_A — `PATCH /api/tickets/{{ticketCancelableCode}}/cancel` → **200**
- [ ] **112** `TC-BIZ-010b` — USER_A — `GET /api/tickets/{{ticketCancelableCode}}` → **200**
- [ ] **113** `TC-BIZ-011` — USER_A — `PATCH /api/tickets/{{ticketCancelableCode}}/cancel` → **409**
- [ ] **114** `TC-BIZ-009b` — STAFF — `GET /api/events/{{publishedEventCode}}/tickets/paid` → **200**
- [ ] **115** `TC-SEC-014` — STAFF — `PATCH /api/tickets/{{ticketUsableCode}}/use` → **200**
- [ ] **116** `TC-BIZ-012` — USER_A — `GET /api/tickets/{{ticketUsableCode}}` → **200**
- [ ] **117** `TC-BIZ-013` — STAFF — `PATCH /api/tickets/{{ticketUsableCode}}/use` → **409**
- [ ] **118** `TC-SEC-011c` — STAFF — `PATCH /api/tickets/{{ticketUserBCode}}/cancel` → **403**
- [ ] **119** `TC-BIZ-010c` — ADMIN — `PATCH /api/tickets/{{ticketUserBCode}}/cancel` → **200**

## Flujo E — Regresión de errores y seguridad avanzada

- [ ] **120** `TC-AUTH-013` — USER_C — `GET /api/auth/me` → **401** · **requiere SQL previo**
- [ ] **121** `TC-AUTH-013b` — ANONYMOUS — `POST /api/auth/login` → **401**
- [ ] **122** `TC-SEC-017` — STAFF — `GET /api/events/{{publishedEventCode}}/tickets/paid` → **403** · **requiere SQL previo**
- [ ] **123** `TC-SEC-018` — ADMIN — `DELETE /api/events/{{publishedEventCode}}` → **403**
- [ ] **124** `TC-SEC-018b` — ANONYMOUS — `DELETE /api/events/{{publishedEventCode}}` → **401**
- [ ] **125** `TC-AUTH-010` — ANONYMOUS — `GET /api/auth/me` → **401**

## Cobertura de las 22 rutas

| Ruta | Método y path | Casos (paso) |
|---|---|---|
| E2E-VEN-01 | `GET /api/venues/{code}` | TC-BIZ-001b (003), TC-HTTP-VEN-404 (004) |
| E2E-VEN-02 | `GET /api/venues/active` | TC-BIZ-001a (002) |
| E2E-EVT-01 | `POST /api/events` | TC-SEC-001a (038), TC-SEC-001b (039), TC-SEC-001c (040), TC-BIZ-003 (041), TC-BIZ-003b (042), TC-BIZ-003c (043), TC-BIZ-003d (044), TC-BIZ-015a (045), TC-BIZ-015b (046), TC-BIZ-015c (047), TC-BIZ-015d (048), TC-BIZ-015e (049), TC-BIZ-015f (050) |
| E2E-EVT-02 | `GET /api/events/{eventCode}` | TC-SEC-015 (051), TC-SEC-015b (052), TC-SEC-004 (053), TC-SEC-004b (054), TC-BIZ-004e (067), TC-HTTP-EVT-404 (071), TC-BIZ-014f (086) |
| E2E-EVT-03 | `GET /api/events/published` | TC-AUTH-015 (037), TC-BIZ-004a (060), TC-BIZ-004f (068) |
| E2E-EVT-04 | `PATCH /api/events/{eventCode}/publish` | TC-SEC-002a (061), TC-SEC-002b (062), TC-BIZ-004 (063), TC-BIZ-004b (064), TC-BIZ-004c (065), TC-BIZ-004d (066) |
| E2E-EVT-05 | `POST /api/events/{eventCode}/artists/{artistId}` | TC-SEC-003 (055), TC-BIZ-005 (056), TC-BIZ-005b (057), TC-BIZ-005c (058), TC-BIZ-005d (059) |
| E2E-EVT-06 | `GET /api/events/by-artist?stageName=` | TC-SEC-005 (069), TC-BIZ-005e (070), TC-HTTP-EVT-404b (072) |
| E2E-ART-01 | `GET /api/artists/{id}` | TC-BIZ-002b (006), TC-HTTP-ART-404 (007), TC-HTTP-ART-400 (008) |
| E2E-ART-02 | `GET /api/artists/by-stage-name?stageName=` | TC-BIZ-002c (009), TC-HTTP-ART-404b (010) |
| E2E-ART-03 | `GET /api/artists/active` | TC-BIZ-002a (005) |
| E2E-USR-01 | `POST /api/users` | TC-AUTH-001 (011), TC-BIZ-006 (012), TC-SEC-016 (013), TC-BIZ-006b (014), TC-HTTP-USR-400 (015), TC-HTTP-JSON-400 (016) |
| E2E-USR-02 | `GET /api/users/by-email?email=` | TC-BIZ-USR-02 (097), TC-SEC-006 (098), TC-BIZ-USR-02b (099), TC-HTTP-USR-400b (100) |
| E2E-USR-03 | `GET /api/users/by-username?username=` | TC-BIZ-USR-03 (101), TC-SEC-007 (102), TC-SEC-007b (103) |
| E2E-TKT-01 | `POST /api/tickets` | TC-SEC-008 (073), TC-SEC-008c (074), TC-SEC-008d (075), TC-BIZ-007 (076), TC-BIZ-007b (077), TC-BIZ-007c (078), TC-HTTP-TKT-400 (079), TC-HTTP-TKT-400b (080), TC-BIZ-014a (081), TC-BIZ-014b (082), TC-BIZ-014c (083), TC-BIZ-014d (084), TC-BIZ-014e (085) |
| E2E-TKT-02 | `GET /api/tickets/{ticketCode}` | TC-BIZ-008 (087), TC-SEC-009 (088), TC-BIZ-008b (089), TC-SEC-009b (090), TC-SEC-009c (091), TC-HTTP-TKT-404 (092), TC-BIZ-010b (112), TC-BIZ-012 (116) |
| E2E-TKT-03 | `GET /api/tickets/by-user?email=` | TC-BIZ-008c (093), TC-SEC-010 (094), TC-BIZ-008d (095), TC-SEC-008b (096), TC-SEC-011b (107) |
| E2E-TKT-04 | `GET /api/events/{eventCode}/tickets/paid` | TC-SEC-013 (108), TC-SEC-013b (109), TC-BIZ-009 (110), TC-BIZ-009b (114), TC-SEC-017 (122) |
| E2E-TKT-05 | `PATCH /api/tickets/{ticketCode}/cancel` | TC-SEC-011 (104), TC-BIZ-010 (111), TC-BIZ-011 (113), TC-SEC-011c (118), TC-BIZ-010c (119) |
| E2E-TKT-06 | `PATCH /api/tickets/{ticketCode}/use` | TC-SEC-012 (105), TC-SEC-012b (106), TC-SEC-014 (115), TC-BIZ-013 (117) |
| E2E-AUTH-01 | `POST /api/auth/login` | TC-AUTH-002 (017), TC-AUTH-003 (018), TC-AUTH-LOGIN-B (019), TC-AUTH-LOGIN-C (020), TC-AUTH-LOGIN-ADMIN (021), TC-AUTH-LOGIN-STAFF (022), TC-AUTH-004 (023), TC-AUTH-005 (024), TC-HTTP-LOGIN-400 (025), TC-AUTH-013b (121) |
| E2E-AUTH-02 | `GET /api/auth/me` | TC-AUTH-006 (026), TC-AUTH-ME-B (027), TC-SEC-016b (028), TC-AUTH-ME-ADMIN (029), TC-AUTH-ME-STAFF (030), TC-AUTH-007 (031), TC-AUTH-008 (032), TC-AUTH-009 (033), TC-AUTH-011 (034), TC-AUTH-011b (035), TC-AUTH-012 (036), TC-AUTH-013 (120), TC-AUTH-010 (125) |

## Al terminar (DoD)

- [ ] 22/22 rutas con al menos un caso nominal en PASS (o FAIL/BLOCKED documentado)
- [ ] Casos 400, 401, 403, 404 y 409 ejecutados
- [ ] Borradores, propiedad de usuarios/tickets y compra ajena verificados con relectura posterior
- [ ] Bitácora diligenciada sin tokens ni contraseñas
- [ ] Ambiente exportado (si se comparte) sin valores en tokens ni contraseñas
- [ ] Defectos y bloqueos registrados; nada sin ejecutar marcado como PASS
