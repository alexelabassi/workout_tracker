package com.thesis.workout;

import static org.assertj.core.api.Assertions.assertThat;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
class WorkoutApplicationTests extends AbstractPostgresIntegrationTest {

    @Autowired
    private DataSource dataSource;

    @Test
    void contextLoads() {
        assertThat(dataSource).isNotNull();
    }

    @Test
    void flywayAppliedInitialSchema() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);

        Integer migrationCount = jdbc.queryForObject(
                "SELECT count(*) FROM flyway_schema_history WHERE success = true", Integer.class);
        assertThat(migrationCount).isNotNull().isGreaterThanOrEqualTo(1);

        Boolean appUsersExists = jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'app_users')",
                Boolean.class);
        assertThat(appUsersExists).isTrue();

        Boolean workoutSetsExists = jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'workout_sets')",
                Boolean.class);
        assertThat(workoutSetsExists).isTrue();
    }
}
