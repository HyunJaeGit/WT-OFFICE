package com.wtoffice.ai;

import java.net.SocketTimeoutException;
import java.net.http.HttpClient;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wtoffice.task.TaskRecord;
import org.springframework.core.env.Environment;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestClientException;

@Component
public class OpenAiResponsesClient {
    private final AiSettings settings;
    private final Environment environment;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public OpenAiResponsesClient(
            AiSettings settings,
            Environment environment,
            ObjectMapper objectMapper,
            RestClient.Builder restClientBuilder) {
        this.settings = settings;
        this.environment = environment;
        this.objectMapper = objectMapper;
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(settings.timeoutSeconds()));
        this.restClient = restClientBuilder
                .baseUrl("https://api.openai.com/v1")
                .requestFactory(requestFactory)
                .build();
    }

    public AiCallResult generate(TaskRecord task) {
        String apiKey = environment.getProperty("OPENAI_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            throw new AiCallException("AUTHENTICATION_FAILED", "서버에 OpenAI API 키가 설정되지 않았습니다.");
        }

        Map<String, Object> body = Map.of(
                "model", settings.model(),
                "instructions", instructionsFor(task.role()),
                "input", "작업 제목: " + task.title() + "\n\n요청 내용:\n" + task.content(),
                "reasoning", Map.of("effort", settings.reasoningEffort()),
                "max_output_tokens", settings.maxOutputTokens(),
                "store", false);

        JsonNode response;
        try {
            response = restClient.post()
                    .uri("/responses")
                    .header("Authorization", "Bearer " + apiKey)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientResponseException exception) {
            throw classifyHttpError(exception);
        } catch (ResourceAccessException exception) {
            Throwable cause = exception.getMostSpecificCause();
            if (cause instanceof HttpTimeoutException || cause instanceof SocketTimeoutException) {
                throw new AiCallException("TIMEOUT", "AI 응답 시간이 초과되었습니다.", exception);
            }
            throw new AiCallException("CONNECTION_FAILED", "AI 서비스에 연결하지 못했습니다.", exception);
        } catch (RestClientException exception) {
            throw new AiCallException("INVALID_RESPONSE", "AI 응답을 읽지 못했습니다.", exception);
        }

        if (response == null) {
            throw new AiCallException("INVALID_RESPONSE", "AI 서비스에서 응답 본문을 받지 못했습니다.");
        }
        String text = extractText(response);
        JsonNode usage = response.path("usage");
        if (!usage.isObject() || !usage.path("input_tokens").canConvertToInt()
                || !usage.path("output_tokens").canConvertToInt()) {
            throw new AiCallException("INVALID_RESPONSE", "AI 응답에서 토큰 사용량을 확인할 수 없습니다.");
        }

        int cachedInputTokens = usage.path("input_tokens_details").path("cached_tokens").asInt(0);
        return new AiCallResult(
                text,
                response.path("model").asText(settings.model()),
                usage.path("input_tokens").asInt(),
                cachedInputTokens,
                usage.path("output_tokens").asInt());
    }

    private AiCallException classifyHttpError(RestClientResponseException exception) {
        return switch (exception.getStatusCode().value()) {
            case 401, 403 -> new AiCallException("AUTHENTICATION_FAILED", "OpenAI 인증에 실패했습니다.", exception);
            case 402 -> new AiCallException("BILLING_LIMIT", "OpenAI 사용 한도 또는 결제 설정을 확인해 주세요.", exception);
            case 408 -> new AiCallException("TIMEOUT", "AI 응답 시간이 초과되었습니다.", exception);
            case 429 -> {
                String providerCode = "";
                try {
                    JsonNode errorBody = objectMapper.readTree(exception.getResponseBodyAsByteArray());
                    providerCode = errorBody.path("error").path("code").asText("");
                } catch (Exception ignored) {
                    // Keep the response body out of logs and use the HTTP status fallback.
                }
                if (providerCode.equals("insufficient_quota") || providerCode.equals("billing_hard_limit_reached")) {
                    yield new AiCallException("BILLING_LIMIT", "OpenAI 사용 한도 또는 결제 설정을 확인해 주세요.", exception);
                }
                yield new AiCallException("RATE_LIMITED", "OpenAI 요청 속도 한도에 도달했습니다.", exception);
            }
            default -> new AiCallException("PROVIDER_ERROR",
                    "OpenAI 요청이 실패했습니다. HTTP " + exception.getStatusCode().value(), exception);
        };
    }

    private String extractText(JsonNode response) {
        ArrayList<String> sections = new ArrayList<>();
        for (JsonNode item : response.path("output")) {
            if (!"message".equals(item.path("type").asText())) {
                continue;
            }
            for (JsonNode content : item.path("content")) {
                if ("output_text".equals(content.path("type").asText())) {
                    String value = content.path("text").asText("");
                    if (!value.isBlank()) {
                        sections.add(value);
                    }
                }
            }
        }
        String text = String.join("\n\n", sections).trim();
        if (text.isBlank()) {
            throw new AiCallException("INVALID_RESPONSE", "AI 응답에 표시할 텍스트 결과가 없습니다.");
        }
        return text;
    }

    private static String instructionsFor(String role) {
        String focus = switch (role) {
            case "기획" -> "요구사항과 제약을 정리하고, 실행 가능한 기획안과 확인 질문을 제시하세요.";
            case "개발" -> "구현 방향과 기술적 고려사항을 제안하세요. 코드를 실행하거나 파일을 수정했다고 주장하지 마세요.";
            case "검토" -> "요청과 제공된 정보를 검토하고, 근거가 있는 의견과 불확실성 및 확인 항목을 구분하세요. 테스트를 실행했다고 주장하지 마세요.";
            default -> throw new AiCallException("INVALID_TASK", "지원하지 않는 담당 역할입니다.");
        };
        return "당신은 WT-OFFICE의 " + role + " 역할 담당자입니다. " + focus
                + " 실제 확인한 사실과 제안·추론을 구분하고, 요청에 답하기 위해 필요한 내용만 간결하게 작성하세요."
                + " 파일 수정, 명령 실행, 외부 서비스 연결은 수행하지 않습니다.";
    }
}
