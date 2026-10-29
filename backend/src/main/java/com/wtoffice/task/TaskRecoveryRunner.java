package com.wtoffice.task;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class TaskRecoveryRunner implements ApplicationRunner {
    private final TaskService taskService;

    public TaskRecoveryRunner(TaskService taskService) {
        this.taskService = taskService;
    }

    @Override
    public void run(ApplicationArguments args) {
        taskService.recoverInterruptedTasks();
    }
}
