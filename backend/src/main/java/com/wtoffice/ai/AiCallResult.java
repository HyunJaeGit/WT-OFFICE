package com.wtoffice.ai;

public record AiCallResult(
        String text,
        String model,
        Integer inputTokens,
        Integer cachedInputTokens,
        Integer outputTokens) {
}
