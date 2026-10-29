package com.wtoffice.task;

import java.time.Instant;

public record TaskRecord(
        String id,
        String title,
        String content,
        String role,
        TaskStatus status,
        String requestId,
        Instant createdAt,
        Instant startedAt,
        Instant finishedAt,
        String resultSummary,
        String errorSummary,
        String errorCode,
        String aiModel,
        Integer inputTokens,
        Integer cachedInputTokens,
        Integer outputTokens) {
}
