# PulsePass

**Plataforma de eventos, artistas y entradas** — Taller de capa de persistencia.

## Integrantes

- Cesar Andres Acosta Torres
- Camilo Andres Hincapie Gomez

## Contexto del proyecto

PulsePass es un caso de estudio académico que modela una plataforma para descubrir eventos y administrar entradas de conciertos, festivales, conferencias y otras actividades. El proyecto se centra en la **capa de persistencia**: el modelo relacional (venues, eventos, artistas, usuarios, perfiles y tickets), las migraciones con Flyway, las entidades JPA, los repositories de Spring Data y las consultas (Query Methods y JPQL).

La integridad de los datos se garantiza en PostgreSQL mediante restricciones `UNIQUE`, `CHECK` y llaves foráneas, y todo se valida con pruebas de integración contra una base de datos real usando Testcontainers.

## Tecnologías

- Java 21
- Spring Boot 4 (Maven)
- Spring Data JPA / Hibernate
- PostgreSQL
- Flyway
- JUnit 5 y Testcontainers
