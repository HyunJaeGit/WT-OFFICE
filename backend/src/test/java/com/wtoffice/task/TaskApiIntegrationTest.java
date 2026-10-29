package com.wtoffice.task;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "server.address=127.0.0.1",
        "spring.datasource.url=jdbc:sqlite:./target/wt-office-test.db"
})
class TaskApiIntegrationTest {
    private static final ParameterizedTypeReference<Map<String, Object>> TASK_BODY = new ParameterizedTypeReference<>() {
    };

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private TaskService taskService;

    @Test
    void createsListsAndLoadsTaskAndReturnsSameTaskForDuplicateRequestId() {
        String requestId = UUID.randomUUID().toString();
        HttpEntity<Map<String, String>> request = createRequest(requestId, "작업 제목", "작업 내용", "기획");

        ResponseEntity<Map<String, Object>> created = restTemplate.exchange(
                "/api/tasks", HttpMethod.POST, request, TASK_BODY);
        ResponseEntity<Map<String, Object>> repeated = restTemplate.exchange(
                "/api/tasks", HttpMethod.POST, request, TASK_BODY);

        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(repeated.getBody().get("id")).isEqualTo(created.getBody().get("id"));
        assertThat(created.getBody().get("status")).isEqualTo("PENDING");

        String id = (String) created.getBody().get("id");
        ResponseEntity<Map<String, Object>> detail = restTemplate.exchange(
                "/api/tasks/" + id, HttpMethod.GET, null, TASK_BODY);
        ResponseEntity<java.util.List<Map<String, Object>>> list = restTemplate.exchange(
                "/api/tasks", HttpMethod.GET, null,
                new ParameterizedTypeReference<java.util.List<Map<String, Object>>>() {
                });

        assertThat(detail.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(detail.getBody().get("content")).isEqualTo("작업 내용");
        assertThat(list.getBody()).anySatisfy(task -> assertThat(task.get("id")).isEqualTo(id));
    }

    @Test
    void rejectsBlankInputAndReusedRequestIdWithDifferentPayload() {
        ResponseEntity<Map<String, Object>> blank = restTemplate.exchange(
                "/api/tasks", HttpMethod.POST,
                createRequest(UUID.randomUUID().toString(), "  ", "내용", "개발"), TASK_BODY);
        assertThat(blank.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        String requestId = UUID.randomUUID().toString();
        restTemplate.exchange("/api/tasks", HttpMethod.POST,
                createRequest(requestId, "첫 제목", "내용", "검토"), TASK_BODY);
        ResponseEntity<Map<String, Object>> conflict = restTemplate.exchange(
                "/api/tasks", HttpMethod.POST,
                createRequest(requestId, "다른 제목", "내용", "검토"), TASK_BODY);

        assertThat(conflict.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void marksRunningTaskAsNeedsReviewWhenRecoveryRuns() {
        ResponseEntity<Map<String, Object>> created = restTemplate.exchange(
                "/api/tasks", HttpMethod.POST,
                createRequest(UUID.randomUUID().toString(), "중단 확인", "서버 복구 처리", "개발"), TASK_BODY);
        String id = (String) created.getBody().get("id");
        HttpEntity<Map<String, String>> running = new HttpEntity<>(Map.of("status", "RUNNING"));
        restTemplate.exchange("/api/tasks/" + id + "/status", HttpMethod.PATCH, running, TASK_BODY);

        taskService.recoverInterruptedTasks();

        ResponseEntity<Map<String, Object>> recovered = restTemplate.exchange(
                "/api/tasks/" + id, HttpMethod.GET, null, TASK_BODY);
        assertThat(recovered.getBody().get("status")).isEqualTo("NEEDS_REVIEW");
        assertThat(recovered.getBody().get("errorSummary").toString()).contains("재시작");
    }

    @Test
    void savesResultForCompletedTaskAndErrorSummaryForFailedTask() {
        String completedId = createTask("완료 결과 저장");
        restTemplate.exchange("/api/tasks/" + completedId + "/status", HttpMethod.PATCH,
                new HttpEntity<>(Map.of("status", "RUNNING")), TASK_BODY);
        restTemplate.exchange("/api/tasks/" + completedId + "/status", HttpMethod.PATCH,
                new HttpEntity<>(Map.of("status", "COMPLETED", "resultSummary", "확인된 결과")), TASK_BODY);
        ResponseEntity<Map<String, Object>> completed = restTemplate.exchange(
                "/api/tasks/" + completedId, HttpMethod.GET, null, TASK_BODY);

        assertThat(completed.getBody().get("status")).isEqualTo("COMPLETED");
        assertThat(completed.getBody().get("resultSummary")).isEqualTo("확인된 결과");

        String failedId = createTask("실패 정보 저장");
        restTemplate.exchange("/api/tasks/" + failedId + "/status", HttpMethod.PATCH,
                new HttpEntity<>(Map.of("status", "RUNNING")), TASK_BODY);
        restTemplate.exchange("/api/tasks/" + failedId + "/status", HttpMethod.PATCH,
                new HttpEntity<>(Map.of("status", "FAILED", "errorSummary", "테스트 오류")), TASK_BODY);
        ResponseEntity<Map<String, Object>> failed = restTemplate.exchange(
                "/api/tasks/" + failedId, HttpMethod.GET, null, TASK_BODY);

        assertThat(failed.getBody().get("status")).isEqualTo("FAILED");
        assertThat(failed.getBody().get("errorSummary")).isEqualTo("테스트 오류");
    }

    private String createTask(String title) {
        ResponseEntity<Map<String, Object>> created = restTemplate.exchange(
                "/api/tasks", HttpMethod.POST,
                createRequest(UUID.randomUUID().toString(), title, "작업 내용", "개발"), TASK_BODY);
        return (String) created.getBody().get("id");
    }

    private static HttpEntity<Map<String, String>> createRequest(
            String requestId, String title, String content, String role) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Idempotency-Key", requestId);
        headers.set("Content-Type", "application/json");
        return new HttpEntity<>(Map.of("title", title, "content", content, "role", role), headers);
    }
}
