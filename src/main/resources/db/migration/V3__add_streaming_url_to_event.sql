-- V3: evento hibrido (FR-EVT-006). Columna opcional; no se modifica V1.
ALTER TABLE events ADD COLUMN streaming_url VARCHAR(500);
