package com.wtoffice.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.ai")
public record AiSettings(
        boolean enabled,
        String model,
        String reasoningEffort,
        int maxOutputTokens,
        int timeoutSeconds) {
}
