package com.wtoffice.ai;

import com.wtoffice.task.TaskRecord;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tasks")
public class TaskExecutionController {
    private final TaskExecutionService executionService;

    public TaskExecutionController(TaskExecutionService executionService) {
        this.executionService = executionService;
    }

    @PostMapping("/{id}/execute")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public TaskRecord execute(@PathVariable String id) {
        return executionService.submit(id);
    }
}
