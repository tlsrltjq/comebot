package com.giseop.comebot.database;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class DatabaseHealthService {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseHealthService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public DatabaseHealthResult check() {
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return new DatabaseHealthResult(true, "PostgreSQL");
        } catch (RuntimeException exception) {
            return new DatabaseHealthResult(false, "PostgreSQL");
        }
    }
}
