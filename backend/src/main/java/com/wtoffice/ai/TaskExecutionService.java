package com.wtoffice.ai;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;

import com.wtoffice.task.TaskRecord;
import com.wtoffice.task.TaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

@Service
public class TaskExecutionService {
    private static final Logger logger = LoggerFactory.getLogger(TaskExecutionService.class);

    private final AiSettings settings;
    private final Environment environment;
    private final OpenAiResponsesClient client;
    private final TaskService taskService;
    private final ExecutorService taskAiExecutor;

    public TaskExecutionService(
            AiSettings settings,
            Environment environment,
            OpenAiResponsesClient client,
            TaskService taskService,
            ExecutorService taskAiExecutor) {
        this.settings = settings;
        this.environment = environment;
        this.client = client;
        this.taskService = taskService;
        this.taskAiExecutor = taskAiExecutor;
    }

    public TaskRecord submit(String taskId) {
        if (!settings.enabled()) {
            throw new AiNotConfiguredException("AI 연결이 비활성 상태입니다. 외부 요청은 전송되지 않았습니다.");
        }
        String apiKey = environment.getProperty("OPENAI_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            throw new AiNotConfiguredException("OPENAI_API_KEY가 설정되지 않았습니다. 외부 요청은 전송되지 않았습니다.");
        }

        TaskRecord running = taskService.startForExecution(taskId);
        try {
            taskAiExecutor.execute(() -> run(running));
            return running;
        } catch (RejectedExecutionException exception) {
            return taskService.failAiTask(taskId, "EXECUTOR_UNAVAILABLE", "AI 실행기를 시작하지 못했습니다.");
        }
    }

    private void run(TaskRecord task) {
        AiCallResult response;
        try {
            response = client.generate(task);
        } catch (AiCallException exception) {
            if (Thread.currentThread().isInterrupted()) {
                logger.warn("AI request was interrupted for task {}; startup recovery will mark it for review", task.id());
                return;
            }
            persistFailure(task.id(), exception.errorCode(), exception.getMessage());
            return;
        }

        try {
            taskService.completeAiTask(task.id(), response.text(), response.model(),
                    response.inputTokens(), response.cachedInputTokens(), response.outputTokens());
        } catch (DataAccessException exception) {
            // Keep the task RUNNING so startup recovery marks it NEEDS_REVIEW instead of claiming an unsaved result.
            logger.error("Could not persist AI result for task {}", task.id());
        }
    }

    private void persistFailure(String taskId, String errorCode, String message) {
        try {
            taskService.failAiTask(taskId, errorCode, message);
        } catch (DataAccessException exception) {
            // The durable RUNNING state is recovered as NEEDS_REVIEW on the next server start.
            logger.error("Could not persist AI failure for task {} ({})", taskId, errorCode);
        }
    }
}
