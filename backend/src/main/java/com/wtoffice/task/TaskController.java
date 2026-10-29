package com.wtoffice.task;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {
    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TaskRecord create(
            @RequestHeader("Idempotency-Key") String requestId,
            @RequestBody CreateTaskRequest request) {
        return taskService.create(request, requestId);
    }

    @GetMapping
    public List<TaskRecord> list() {
        return taskService.list();
    }

    @GetMapping("/{id}")
    public TaskRecord get(@PathVariable String id) {
        return taskService.get(id);
    }

    @PatchMapping("/{id}/status")
    public TaskRecord updateStatus(@PathVariable String id, @RequestBody UpdateTaskStatusRequest request) {
        return taskService.updateStatus(id, request);
    }
}
