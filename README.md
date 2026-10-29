# WT-OFFICE

사무실 업무를 지원하는 인터페이스와 로컬 작업 관리 환경입니다. 작업 요청은 SQLite에 저장됩니다. AI 호출은 기본 비활성이며, 작업 상세에서 사용자가 전송을 확인한 경우에만 실행됩니다.

## 구성

- `frontend/`: React, TypeScript, Vite 프런트엔드
- `backend/`: Java 21, Spring Boot 3.5, SQLite 작업 API
- `.github/workflows/`: 정적 프런트엔드 빌드 및 GitHub Pages 배포

GitHub Pages는 정적 프런트엔드만 제공합니다. 로컬 Spring Boot 백엔드와 SQLite 데이터베이스는 Pages에서 실행되지 않습니다. 배포된 페이지는 개발 PC의 `localhost` 백엔드에 연결할 수 없으므로, 저장 기능은 프런트엔드와 백엔드를 같은 PC에서 실행할 때 사용할 수 있습니다.

## 개발 환경

- Java 21 및 Maven 3.6.3 이상
- Node.js 20.19+ 또는 22.12+ (Node.js 24 LTS 권장) 및 npm
- 현재 확인한 값: Eclipse Temurin OpenJDK 21.0.12.1, Node.js 26.13.0, npm 11.20.0. Maven은 설치되어 있지 않습니다.

### 백엔드 실행

PowerShell에서 저장소 루트 기준:

```powershell
cd backend
mvn spring-boot:run
```

백엔드 통합 테스트를 실행하려면 `backend/`에서 `mvn test`를 실행합니다. 테스트용 DB는 `backend/target/wt-office-test.db`이며 운영 DB와 분리되어 있습니다.

서버는 `127.0.0.1:8080`에서만 요청을 받습니다. 작업 API는 `GET/POST /api/tasks`, `GET /api/tasks/{id}`, `PATCH /api/tasks/{id}/status`, `POST /api/tasks/{id}/execute`, `GET /api/ai/status`입니다. 작업 생성은 `Idempotency-Key` 헤더에 UUID 요청 식별자를 보내며, 같은 식별자와 같은 내용을 다시 제출하면 기존 작업을 반환합니다. 같은 키에 다른 내용을 보내면 충돌 응답을 반환합니다. AI 실행 API는 작업 상세에서 한 번 더 전송 확인을 거치며, 동시에 하나의 작업만 실행합니다.

### 프런트엔드 실행

별도 PowerShell 창에서 저장소 루트 기준:

```powershell
cd frontend
Copy-Item .env.example .env.local
npm install
npm run dev
```

기본 API 주소는 `http://127.0.0.1:8080`입니다. 변경할 경우 `frontend/.env.local`의 `VITE_API_BASE_URL` 값을 지정합니다. `.env.local`은 Git에 포함되지 않습니다. 프로덕션 프런트엔드 빌드는 다음과 같습니다.

```powershell
npm run build
```

## 작업 데이터와 백업

데이터베이스 파일은 `backend/wt-office.db`에 생성됩니다. 이 파일은 Git에서 제외됩니다. 작업 내용과 결과 요약, 모델명, 입력/캐시 입력/출력 토큰 수, 오류 코드는 로컬 디스크에 저장됩니다. AI가 꺼져 있으면 새 작업은 `PENDING`(대기) 상태로 남습니다.

서버 재시작 후 앱이 저장 파일을 다시 읽습니다. 이전 프로세스 중 `RUNNING` 상태였던 작업이 있으면 앱 시작 시 `NEEDS_REVIEW`(확인 필요)로 바꾸고 자동 재실행하지 않습니다.

백업과 복원 시 SQLite 파일을 직접 복사하거나 교체하기 전에 백엔드를 종료하세요. 백업 파일이 완전한지 확인하고, 복원 전 현재 DB를 별도 보관하세요. 앱이 실행 중일 때 파일을 복사하면 최신 변경 사항이 빠지거나 일관성이 깨질 수 있습니다. 이 파일을 지우면 저장된 작업이 삭제되므로 초기화 용도로 삭제하지 마세요. 백업 파일은 저장소 외부의 안전한 위치에 보관합니다.

## 상태와 AI 연결

지원 상태는 대기(`PENDING`), 진행(`RUNNING`), 완료(`COMPLETED`), 실패(`FAILED`), 확인 필요(`NEEDS_REVIEW`)입니다. 완료 상태에는 결과 요약이, 실패 상태에는 오류 요약이 필요합니다. 잘못된 입력은 400, 존재하지 않는 작업은 404, 멱등 키 충돌은 409, 저장소 오류는 500 응답으로 처리합니다.

실제 연동은 서버에서 OpenAI Responses API를 직접 호출합니다. 브라우저에 API 키를 보내지 않으며, Codex의 파일·명령 실행 도구는 연결하지 않습니다. 기본 설정은 `AI_ENABLED=false`, 모델 `gpt-6-luna`, 추론 강도 `low`, 출력 상한 1500 토큰, 요청 시간 제한 45초입니다. AI 사용을 허용하려면 API 키를 서버 프로세스 환경 변수로 설정하고 `AI_ENABLED=true`로 실행하세요. 키를 채팅, 브라우저 설정, 저장소 파일에 입력하지 말고 `.env` 파일에도 기록하지 마세요.

Git Bash 예시(키 입력은 화면에 표시되지 않고 현재 터미널 세션에만 적용됩니다):

```bash
read -rsp "OpenAI API key: " OPENAI_API_KEY
printf '\\n'
export OPENAI_API_KEY
export AI_ENABLED=true
cd backend
mvn spring-boot:run
```

PowerShell 예시:

```powershell
$secureKey = Read-Host "OpenAI API key" -AsSecureString
$env:OPENAI_API_KEY = [System.Net.NetworkCredential]::new("", $secureKey).Password
$env:AI_ENABLED = "true"
cd backend
mvn spring-boot:run
```

작업 생성 자체는 AI 요청을 보내지 않습니다. 상세 화면의 **GPT-6 Luna 실행** 버튼을 누르고 확인해야 외부 전송이 시작됩니다. OpenAI API 사용량은 ChatGPT 요금제의 Codex 한도와 별도로 API 과금에 반영됩니다. 응답의 모델명과 입력·캐시 입력·출력 토큰 수를 작업과 함께 저장하고 상세 화면에 표시합니다. 현재 단가는 바뀔 수 있으므로 [공식 API 가격표](https://developers.openai.com/api/docs/pricing)를 확인하세요.

연결 오류, 시간 초과, 인증 실패, 속도 제한, 결제·사용 한도, 응답 형식 오류는 서로 다른 오류 코드로 저장됩니다. 결과 저장이 실패하면 완료로 바꾸지 않고 진행 상태를 남겨 다음 서버 시작 때 확인 필요로 전환합니다. 자동 재실행하지 않습니다. 실제 계정 키가 없어 이 저장소의 API 연결과 실제 요청은 아직 검증되지 않았습니다.

## GitHub Pages

Vite base 경로는 저장소 경로인 `/WT-OFFICE/`로 설정되어 있습니다. `main`의 프런트엔드 변경은 GitHub Actions에서 빌드하고 Pages에 배포합니다. 배포 전에 저장소 **Settings → Pages → Build and deployment → Source**를 **GitHub Actions**로 설정해야 합니다. Pages에서는 백엔드나 로컬 SQLite를 실행하지 않습니다.

## 운영 규칙

- 작업 지침은 [AGENTS.md](AGENTS.md)를 따릅니다.
- `release note.md`는 사용자가 기록을 요청했을 때만 갱신합니다.
- 커밋, 푸시, 브랜치 변경, 저장소 설정 변경, 실제 배포는 사용자 요청이 있어야 합니다.
- 다음 Phase는 사용자 승인 후 진행합니다.
