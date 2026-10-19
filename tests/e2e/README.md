# PulsePass — Pruebas E2E manuales con Postman (Fase 5)

Pruebas de caja negra HTTP contra la app real + PostgreSQL. **Manual:** sin scripts, sin Runner, sin Newman.

## Contenido

| Archivo | Para qué |
|---|---|
| `postman/PulsePass-Manual-E2E.postman_collection.json` | 22 rutas en 10 carpetas, cada request con descripción (ID, objetivo, actor, esperado, revisión) |
| `postman/PulsePass-local.postman_environment.json` | Plantilla de variables **sin secretos** |
| `fixtures/seed-e2e.sql` | Venues de prueba (solo `pulsepass_e2e`) |
| `fixtures/README.md` | Fixtures, SQL de casos especiales y reinicio |
| `checklist-e2e-manual.md` | Orden de ejecución paso a paso + cobertura 22/22 |
| `registro-ejecucion-manual.md` | Bitácora para anotar esperado vs observado |

## Preparación

1. Crear la base `pulsepass_e2e` (dueño `pulsepass`).
2. Arrancar la app con: `PULSEPASS_DB_URL=jdbc:postgresql://localhost:5432/pulsepass_e2e`, `PULSEPASS_JWT_SECRET_BASE64` (exclusivo de E2E), `PULSEPASS_SEED_ADMIN_PASSWORD`, `PULSEPASS_SEED_STAFF_PASSWORD` y perfil `dev`.
3. Ejecutar `fixtures/seed-e2e.sql` sobre `pulsepass_e2e`.
4. Importar colección y ambiente en Postman; seleccionar el ambiente; escribir contraseñas en **Current value**.

## Ejecución

- Seguir `checklist-e2e-manual.md` en orden (el número 001–125 está en el nombre de cada request).
- Tras cada login, copiar `accessToken` a la variable del actor (`userAToken`, `userBToken`, `userCToken`, `adminToken`, `staffToken`).
- Copiar los códigos indicados (`artistId`, `ticketCancelableCode`, `ticketUsableCode`, `ticketUserBCode`).
- Los tokens duran 15 min: ante un 401 inesperado, repetir el login del actor.
- Política de recurso ajeno/no visible: **403**.
- Un 2xx no es PASS por sí solo: revisar cuerpo y, si hubo mutación o denegación, la relectura posterior.
- Si falla un prerrequisito, marcar los casos dependientes como **BLOCKED**.

## Repetir

Cambiar `runId` (r01 → r02) o recrear `pulsepass_e2e` (ver `fixtures/README.md`). Nunca borrar otra base.

## Seguridad

No versionar tokens ni contraseñas. Si exportas el ambiente, deja vacíos los valores secretos.
