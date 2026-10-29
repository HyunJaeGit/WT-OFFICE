package com.wtoffice.task;

import java.util.HashSet;
import java.util.Set;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TaskSchemaMigration implements ApplicationRunner {
    private final JdbcTemplate jdbcTemplate;

    public TaskSchemaMigration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        Set<String> columns = new HashSet<>(jdbcTemplate.query(
                "PRAGMA table_info(tasks)",
                (resultSet, rowNumber) -> resultSet.getString("name")));
        addColumnIfMissing(columns, "error_code", "TEXT");
        addColumnIfMissing(columns, "ai_model", "TEXT");
        addColumnIfMissing(columns, "input_tokens", "INTEGER");
        addColumnIfMissing(columns, "cached_input_tokens", "INTEGER");
        addColumnIfMissing(columns, "output_tokens", "INTEGER");
    }

    private void addColumnIfMissing(Set<String> existingColumns, String name, String sqlType) {
        if (existingColumns.add(name)) {
            jdbcTemplate.execute("ALTER TABLE tasks ADD COLUMN " + name + " " + sqlType);
        }
    }
}
