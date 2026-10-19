# Fixtures E2E

| Fixture | Cómo se crea | Variable Postman |
|---|---|---|
| Venue activo `VEN-E2E-01` (capacidad 500) | `seed-e2e.sql` | `venueActiveCode` |
| Venue activo `VEN-E2E-MINI` (capacidad 1) | `seed-e2e.sql` | `venueMiniCode` |
| Venue inactivo `VEN-E2E-OFF` | `seed-e2e.sql` | `venueInactiveCode` |
| Artistas activos (Solar Beat, …) | Migración Flyway V2 | `artistId` (copiado de la API), `artistStageName` |
| ADMIN `miguel` / STAFF `laura` | `DevSecurityDataSeeder` (perfil `dev` + `PULSEPASS_SEED_*_PASSWORD`) | `adminIdentifier`, `staffIdentifier` |
| USER_A, USER_B | Registro público en la colección | `userA*`, `userB*` |
| INACTIVE_USER (USER_C) | Registro público + SQL de desactivación (flujo E) | `userC*` |
| Eventos PUB / DRAFT / MINI / AGE | Creados por ADMIN en el flujo C | `publishedEventCode`, `draftEventCode`, `miniEventCode`, `ageEventCode` |
| Tickets T1 / T2 / de B | Comprados en el flujo D | `ticketCancelableCode`, `ticketUsableCode`, `ticketUserBCode` |

Ningún privilegio se asigna por `POST /api/users`. No hay contraseñas en este repositorio.

## SQL de casos especiales (solo `pulsepass_e2e`)

```sql
-- TC-AUTH-013: desactivar USER_C (cambia r01 por tu runId)
UPDATE users SET active = false WHERE username = 'userc_r01';

-- TC-SEC-017: revocar y luego restaurar el rol STAFF de laura
DELETE FROM user_roles WHERE role = 'STAFF' AND user_id = (SELECT id FROM users WHERE username = 'laura');
INSERT INTO user_roles (user_id, role) SELECT id, 'STAFF' FROM users WHERE username = 'laura';
```

## Repetir desde cero

Opción A (rápida): cambia `runId` en el ambiente (r01 → r02). Todos los usuarios, eventos y tickets nuevos tendrán códigos distintos; los venues y cuentas privilegiadas se reutilizan.

Opción B (limpia): borra y recrea **solo** la base E2E, arranca la app (Flyway + seeder) y vuelve a ejecutar `seed-e2e.sql`:

```sql
-- conectado a la base postgres, NUNCA a pulsepass ni a otra base
DROP DATABASE IF EXISTS pulsepass_e2e;
CREATE DATABASE pulsepass_e2e OWNER pulsepass;
```
