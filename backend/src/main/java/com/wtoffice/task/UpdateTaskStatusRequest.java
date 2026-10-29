package com.wtoffice.task;

public record UpdateTaskStatusRequest(
        String status,
        String resultSummary,
        String errorSummary,
        String errorCode,
        String aiModel,
        Integer inputTokens,
        Integer cachedInputTokens,
        Integer outputTokens) {
}
