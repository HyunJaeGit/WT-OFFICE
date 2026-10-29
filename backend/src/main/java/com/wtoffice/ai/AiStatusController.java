package com.wtoffice.ai;

import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
public class AiStatusController {
    private final AiSettings settings;
    private final Environment environment;

    public AiStatusController(AiSettings settings, Environment environment) {
        this.settings = settings;
        this.environment = environment;
    }

    @GetMapping("/status")
    public AiStatus status() {
        boolean configured = !isBlank(environment.getProperty("OPENAI_API_KEY"));
        boolean available = settings.enabled() && configured;
        String message = available
                ? "AI 요청은 사용자가 작업 화면에서 실행할 때만 전송됩니다."
                : settings.enabled()
                        ? "OPENAI_API_KEY 환경 변수를 설정한 뒤 백엔드를 다시 시작해 주세요."
                        : "AI 연결은 비활성 상태입니다. 외부 요청이나 사용량이 발생하지 않습니다.";
        return new AiStatus(settings.enabled(), configured, available,
                settings.model(), settings.reasoningEffort(), message);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
