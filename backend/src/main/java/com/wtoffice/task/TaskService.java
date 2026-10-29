package com.wtoffice.task;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskService {
    private final TaskRepository repository;

    public TaskService(TaskRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public synchronized TaskRecord create(CreateTaskRequest request, String requestId) {
        if (request == null) {
            throw new InvalidTaskException("작업 요청 내용을 입력해 주세요.");
        }
        validateText(request.title(), "작업 제목", 120);
        validateText(request.content(), "작업 내용", 5000);
        TaskRole role;
        try {
            role = TaskRole.fromLabel(request.role());
        } catch (IllegalArgumentException exception) {
            throw new InvalidTaskException(exception.getMessage());
        }
        try {
            UUID.fromString(requestId);
        } catch (IllegalArgumentException exception) {
            throw new InvalidTaskException("요청 식별자는 UUID 형식이어야 합니다.");
        }

        Optional<TaskRecord> existing = repository.findByRequestId(requestId);
        if (existing.isPresent()) {
            TaskRecord task = existing.get();
            if (!task.title().equals(request.title().trim())
                    || !task.content().equals(request.content().trim())
                    || !task.role().equals(role.label())) {
                throw new DuplicateRequestException("같은 요청 식별자가 다른 작업 내용에 이미 사용되었습니다.");
            }
            return task;
        }

        return repository.insert(request.title().trim(), request.content().trim(), role.label(), requestId);
    }

    public List<TaskRecord> list() {
        return repository.findAll();
    }

    public TaskRecord get(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new TaskNotFoundException("작업을 찾을 수 없습니다."));
    }

    @Transactional
    public synchronized TaskRecord updateStatus(String id, UpdateTaskStatusRequest request) {
        if (request == null) {
            throw new InvalidTaskException("작업 상태 정보를 입력해 주세요.");
        }
        TaskRecord current = get(id);
        TaskStatus next;
        try {
            next = TaskStatus.valueOf(request.status());
        } catch (RuntimeException exception) {
            throw new InvalidTaskException("지원하지 않는 작업 상태입니다.");
        }

        boolean validTransition = (current.status() == TaskStatus.PENDING && next == TaskStatus.RUNNING)
                || (current.status() == TaskStatus.RUNNING
                        && (next == TaskStatus.COMPLETED || next == TaskStatus.FAILED));
        if (!validTransition) {
            throw new InvalidTaskException("허용되지 않는 작업 상태 변경입니다.");
        }
        if (next == TaskStatus.COMPLETED && (request.resultSummary() == null || request.resultSummary().isBlank())) {
            throw new InvalidTaskException("완료 처리에는 결과 요약이 필요합니다.");
        }
        if (next == TaskStatus.FAILED && (request.errorSummary() == null || request.errorSummary().isBlank())) {
            throw new InvalidTaskException("실패 처리에는 오류 요약이 필요합니다.");
        }
        if (next == TaskStatus.RUNNING && (hasText(request.resultSummary()) || hasText(request.errorSummary()))) {
            throw new InvalidTaskException("진행 상태에는 결과나 오류 요약을 저장할 수 없습니다.");
        }
        if (request.resultSummary() != null && request.resultSummary().length() > 20000) {
            throw new InvalidTaskException("결과 요약은 20000자 이하여야 합니다.");
        }
        if (request.errorSummary() != null && request.errorSummary().length() > 4000) {
            throw new InvalidTaskException("오류 요약은 4000자 이하여야 합니다.");
        }

        if (next == TaskStatus.RUNNING) {
            if (!repository.claimPendingTask(id)) {
                throw new TaskConflictException("이미 실행 중인 작업이 있거나 이 작업을 시작할 수 없습니다.");
            }
        } else {
            repository.updateStatus(id, next, request.resultSummary(), request.errorSummary());
        }
        return get(id);
    }

    @Transactional
    public synchronized TaskRecord startForExecution(String id) {
        TaskRecord current = get(id);
        if (current.status() != TaskStatus.PENDING || !repository.claimPendingTask(id)) {
            throw new TaskConflictException("이 작업을 실행할 수 없습니다. 현재 상태를 확인해 주세요.");
        }
        return get(id);
    }

    @Transactional
    public synchronized TaskRecord completeAiTask(String id, String result, String model,
            Integer inputTokens, Integer cachedInputTokens, Integer outputTokens) {
        requireRunning(id);
        repository.updateStatus(id, TaskStatus.COMPLETED, result, null, null, model,
                inputTokens, cachedInputTokens, outputTokens);
        return get(id);
    }

    @Transactional
    public synchronized TaskRecord failAiTask(String id, String errorCode, String errorSummary) {
        requireRunning(id);
        repository.updateStatus(id, TaskStatus.FAILED, null, errorSummary, errorCode,
                null, null, null, null);
        return get(id);
    }

    public int recoverInterruptedTasks() {
        return repository.markRunningTasksForReview("서버가 재시작되어 이전 실행 상태를 확인할 수 없습니다.");
    }

    private static void validateText(String value, String fieldName, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new InvalidTaskException(fieldName + "을(를) 입력해 주세요.");
        }
        if (value.trim().length() > maxLength) {
            throw new InvalidTaskException(fieldName + "은(는) " + maxLength + "자 이하여야 합니다.");
        }
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private void requireRunning(String id) {
        if (get(id).status() != TaskStatus.RUNNING) {
            throw new TaskConflictException("진행 중인 작업만 완료하거나 실패로 변경할 수 있습니다.");
        }
    }
}
