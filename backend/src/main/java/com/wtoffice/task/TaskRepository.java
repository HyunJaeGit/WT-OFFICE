package com.wtoffice.task;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class TaskRepository {
    private static final RowMapper<TaskRecord> TASK_ROW_MAPPER = TaskRepository::mapTask;

    private final JdbcTemplate jdbcTemplate;

    public TaskRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public TaskRecord insert(String title, String content, String role, String requestId) {
        TaskRecord task = new TaskRecord(
                UUID.randomUUID().toString(), title, content, role, TaskStatus.PENDING,
                requestId, Instant.now(), null, null, null, null, null, null, null, null, null);
        jdbcTemplate.update(
                "INSERT INTO tasks (id, title, content, role, status, request_id, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
                task.id(), task.title(), task.content(), task.role(), task.status().name(),
                task.requestId(), task.createdAt().toString());
        return task;
    }

    public List<TaskRecord> findAll() {
        return jdbcTemplate.query("SELECT * FROM tasks ORDER BY created_at DESC", TASK_ROW_MAPPER);
    }

    public Optional<TaskRecord> findById(String id) {
        return jdbcTemplate.query("SELECT * FROM tasks WHERE id = ?", TASK_ROW_MAPPER, id)
                .stream().findFirst();
    }

    public Optional<TaskRecord> findByRequestId(String requestId) {
        return jdbcTemplate.query("SELECT * FROM tasks WHERE request_id = ?", TASK_ROW_MAPPER, requestId)
                .stream().findFirst();
    }

    public void updateStatus(String id, TaskStatus status, String resultSummary, String errorSummary) {
        updateStatus(id, status, resultSummary, errorSummary, null, null, null, null, null);
    }

    public boolean claimPendingTask(String id) {
        int updated = jdbcTemplate.update("""
                UPDATE tasks
                SET status = 'RUNNING', started_at = ?
                WHERE id = ? AND status = 'PENDING'
                  AND NOT EXISTS (SELECT 1 FROM tasks WHERE status = 'RUNNING')
                """, Instant.now().toString(), id);
        return updated == 1;
    }

    public void updateStatus(
            String id,
            TaskStatus status,
            String resultSummary,
            String errorSummary,
            String errorCode,
            String aiModel,
            Integer inputTokens,
            Integer cachedInputTokens,
            Integer outputTokens) {
        Instant now = Instant.now();
        jdbcTemplate.update("""
                UPDATE tasks
                SET status = ?,
                    started_at = CASE WHEN ? = 'RUNNING' THEN COALESCE(started_at, ?) ELSE started_at END,
                    finished_at = CASE WHEN ? IN ('COMPLETED', 'FAILED') THEN ? ELSE finished_at END,
                    result_summary = ?,
                    error_summary = ?,
                    error_code = ?,
                    ai_model = ?,
                    input_tokens = ?,
                    cached_input_tokens = ?,
                    output_tokens = ?
                WHERE id = ?
                """,
                status.name(), status.name(), now.toString(), status.name(), now.toString(),
                resultSummary, errorSummary, errorCode, aiModel, inputTokens, cachedInputTokens, outputTokens, id);
    }

    public int markRunningTasksForReview(String reason) {
        return jdbcTemplate.update(
                "UPDATE tasks SET status = 'NEEDS_REVIEW', error_summary = ? WHERE status = 'RUNNING'",
                reason);
    }

    private static TaskRecord mapTask(ResultSet resultSet, int rowNumber) throws SQLException {
        return new TaskRecord(
                resultSet.getString("id"),
                resultSet.getString("title"),
                resultSet.getString("content"),
                resultSet.getString("role"),
                TaskStatus.valueOf(resultSet.getString("status")),
                resultSet.getString("request_id"),
                parseInstant(resultSet.getString("created_at")),
                parseInstant(resultSet.getString("started_at")),
                parseInstant(resultSet.getString("finished_at")),
                resultSet.getString("result_summary"),
                resultSet.getString("error_summary"),
                resultSet.getString("error_code"),
                resultSet.getString("ai_model"),
                nullableInteger(resultSet, "input_tokens"),
                nullableInteger(resultSet, "cached_input_tokens"),
                nullableInteger(resultSet, "output_tokens"));
    }

    private static Integer nullableInteger(ResultSet resultSet, String column) throws SQLException {
        int value = resultSet.getInt(column);
        return resultSet.wasNull() ? null : value;
    }

    private static Instant parseInstant(String value) {
        return value == null ? null : Instant.parse(value);
    }
}
