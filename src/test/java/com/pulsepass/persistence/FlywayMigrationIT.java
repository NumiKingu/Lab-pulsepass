package com.pulsepass.persistence;

import com.pulsepass.support.AbstractPostgresIT;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


class FlywayMigrationIT extends AbstractPostgresIT {

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private Environment environment;

    @Test
    @DisplayName("QT-001: Flyway aplica V1, V2 y V3 en orden desde una base vacia")
    void appliesAllMigrationsFromEmptyDatabase() {
        List<String> versions = jdbc.queryForList(
                "select version from flyway_schema_history where success = true and version is not null order by installed_rank",
                String.class);

        assertThat(versions).containsExactly("1", "2", "3");
    }

    @Test
    @DisplayName("QT-002: Hibernate esta en modo validate (no crea ni actualiza el esquema)")
    void hibernateOnlyValidatesSchema() {
        assertThat(environment.getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("validate");

    }

    @Test
    @DisplayName("V1 crea las siete tablas del modelo")
    void createsAllTables() {
        List<String> tables = jdbc.queryForList(
                "select table_name from information_schema.tables where table_schema = current_schema()",
                String.class);

        assertThat(tables).contains("venues", "events", "artists", "event_artists",
                "users", "user_profiles", "tickets");
    }

    @Test
    @DisplayName("V2 inserta el catalogo inicial de cinco artistas")
    void insertsInitialArtists() {
        List<String> names = jdbc.queryForList("select stage_name from artists", String.class);

        assertThat(names).containsExactlyInAnyOrder(
                "Solar Beat", "Neon Waves", "Caribbean Sound", "Ocean Drive", "Digital Pulse");
    }

    @Test
    @DisplayName("FR-EVT-006 / V3: events.streaming_url es VARCHAR(500) nullable")
    void addsNullableStreamingUrlColumn() {
        var column = jdbc.queryForMap("""
                select is_nullable, character_maximum_length
                from information_schema.columns
                where table_schema = current_schema()
                  and table_name = 'events'
                  and column_name = 'streaming_url'
                """);

        assertThat(column.get("is_nullable")).isEqualTo("YES");
        assertThat(((Number) column.get("character_maximum_length")).intValue()).isEqualTo(500);
    }
}
