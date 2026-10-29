package com.wtoffice.ai;

public record AiStatus(boolean enabled, boolean configured, boolean available,
        String model, String reasoningEffort, String message) {
}
