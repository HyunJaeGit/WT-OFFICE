package com.wtoffice.ai;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiExecutionConfiguration {
    @Bean(destroyMethod = "shutdown")
    public ExecutorService taskAiExecutor() {
        return Executors.newSingleThreadExecutor(task -> {
            Thread thread = new Thread(task, "wt-office-ai-worker");
            thread.setDaemon(true);
            return thread;
        });
    }
}
