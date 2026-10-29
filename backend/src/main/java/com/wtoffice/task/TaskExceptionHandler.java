package com.wtoffice.task;

import java.util.Map;

import com.wtoffice.ai.AiNotConfiguredException;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class TaskExceptionHandler {
    @ExceptionHandler(InvalidTaskException.class)
    public ResponseEntity<Map<String, String>> invalidTask(InvalidTaskException exception) {
        return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
    }

    @ExceptionHandler(DuplicateRequestException.class)
    public ResponseEntity<Map<String, String>> duplicateRequest(DuplicateRequestException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", exception.getMessage()));
    }

    @ExceptionHandler(TaskConflictException.class)
    public ResponseEntity<Map<String, String>> taskConflict(TaskConflictException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", exception.getMessage()));
    }

    @ExceptionHandler(AiNotConfiguredException.class)
    public ResponseEntity<Map<String, String>> aiNotConfigured(AiNotConfiguredException exception) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("message", exception.getMessage()));
    }

    @ExceptionHandler(TaskNotFoundException.class)
    public ResponseEntity<Map<String, String>> notFound(TaskNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", exception.getMessage()));
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<Map<String, String>> missingRequestId() {
        return ResponseEntity.badRequest().body(Map.of("message", "요청 식별자가 필요합니다. 다시 시도해 주세요."));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> malformedRequest() {
        return ResponseEntity.badRequest().body(Map.of("message", "요청 본문을 읽을 수 없습니다. 입력 내용을 확인해 주세요."));
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Map<String, String>> storageFailure() {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "작업을 저장하지 못했습니다. 데이터베이스 경로와 디스크 상태를 확인해 주세요."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> unexpectedFailure() {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "요청을 처리하지 못했습니다. 잠시 후 다시 시도해 주세요."));
    }
}

class InvalidTaskException extends RuntimeException {
    InvalidTaskException(String message) {
        super(message);
    }
}

class DuplicateRequestException extends RuntimeException {
    DuplicateRequestException(String message) {
        super(message);
    }
}

class TaskNotFoundException extends RuntimeException {
    TaskNotFoundException(String message) {
        super(message);
    }
}

class TaskConflictException extends RuntimeException {
    TaskConflictException(String message) {
        super(message);
    }
}
