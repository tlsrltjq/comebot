package com.giseop.comebot.database;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.jdbc.core.JdbcTemplate;

class DatabaseHealthServiceTest {

    @Test
    void checkReturnsConnectedTrueWhenDatabaseQuerySucceeds() {
        JdbcTemplate jdbcTemplate = org.mockito.Mockito.mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForObject("SELECT 1", Integer.class)).thenReturn(1);

        DatabaseHealthResult result = new DatabaseHealthService(jdbcTemplate).check();

        assertThat(result.connected()).isTrue();
        assertThat(result.database()).isEqualTo("PostgreSQL");
    }

    @Test
    void checkReturnsConnectedFalseWhenDatabaseQueryFails() {
        JdbcTemplate jdbcTemplate = org.mockito.Mockito.mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForObject("SELECT 1", Integer.class))
                .thenThrow(new CannotGetJdbcConnectionException("connection failed"));

        DatabaseHealthResult result = new DatabaseHealthService(jdbcTemplate).check();

        assertThat(result.connected()).isFalse();
        assertThat(result.database()).isEqualTo("PostgreSQL");
    }
}
