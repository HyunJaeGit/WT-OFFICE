# WT-OFFICE

사무실 업무를 지원하는 인터페이스와 로컬 작업 관리 환경입니다. AI 실행 연결은 승인 전까지 비활성 상태이며, 현재 작업 요청은 SQLite에 저장되어 대기 상태로 유지됩니다.

## 구성

- `frontend/`: React, TypeScript, Vite 프런트엔드
- `backend/`: Java 21, Spring Boot 3.5, SQLite 작업 API
- `.github/workflows/`: 정적 프런트엔드 빌드 및 GitHub Pages 배포

GitHub Pages는 정적 프런트엔드만 제공합니다. 로컬 Spring Boot 백엔드와 SQLite 데이터베이스는 Pages에서 실행되지 않습니다. 배포된 페이지는 개발 PC의 `localhost` 백엔드에 연결할 수 없으므로, 저장 기능은 프런트엔드와 백엔드를 같은 PC에서 실행할 때 사용할 수 있습니다.

## 개발 환경

- Java 21 및 Maven 3.6.3 이상
- Node.js 20.19+ 또는 22.12+ (Node.js 24 LTS 권장) 및 npm
- 현재 실행 환경에서 확인한 값: Eclipse Temurin OpenJDK 21.0.12.1. Node.js/npm/Maven은 확인되지 않아 이 환경에서 빌드와 실행은 검증하지 못했습니다.

### 백엔드 실행

PowerShell에서 저장소 루트 기준:

```powershell
cd backend
mvn spring-boot:run
```

백엔드 통합 테스트를 실행하려면 `backend/`에서 `mvn test`를 실행합니다. 테스트용 DB는 `backend/target/wt-office-test.db`이며 운영 DB와 분리되어 있습니다.

서버는 `127.0.0.1:8080`에서만 요청을 받습니다. 작업 API는 `GET/POST /api/tasks`, `GET /api/tasks/{id}`, `PATCH /api/tasks/{id}/status`입니다. 작업 생성은 `Idempotency-Key` 헤더에 UUID 요청 식별자를 보내며, 같은 식별자와 같은 내용을 다시 제출하면 기존 작업을 반환합니다. 같은 키에 다른 내용을 보내면 충돌 응답을 반환합니다.

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

데이터베이스 파일은 `backend/wt-office.db`에 생성됩니다. 이 파일은 Git에서 제외됩니다. 작업 내용과 결과 요약은 로컬 디스크에 저장되며, 현재 AI 실행은 연결되지 않았으므로 새 작업은 `PENDING`(대기) 상태로 남습니다.

서버 재시작 후 앱이 저장 파일을 다시 읽습니다. 이전 프로세스 중 `RUNNING` 상태였던 작업이 있으면 앱 시작 시 `NEEDS_REVIEW`(확인 필요)로 바꾸고 자동 재실행하지 않습니다.

백업과 복원 시 SQLite 파일을 직접 복사하거나 교체하기 전에 백엔드를 종료하세요. 백업 파일이 완전한지 확인하고, 복원 전 현재 DB를 별도 보관하세요. 앱이 실행 중일 때 파일을 복사하면 최신 변경 사항이 빠지거나 일관성이 깨질 수 있습니다. 이 파일을 지우면 저장된 작업이 삭제되므로 초기화 용도로 삭제하지 마세요. 백업 파일은 저장소 외부의 안전한 위치에 보관합니다.

## 상태와 AI 연결

지원 상태는 대기(`PENDING`), 진행(`RUNNING`), 완료(`COMPLETED`), 실패(`FAILED`), 확인 필요(`NEEDS_REVIEW`)입니다. 완료 상태에는 결과 요약이, 실패 상태에는 오류 요약이 필요합니다. 잘못된 입력은 400, 존재하지 않는 작업은 404, 멱등 키 충돌은 409, 저장소 오류는 500 응답으로 처리합니다.

Phase 4에서 승인된 AI 실행 방식이 정해질 때까지 인증 정보, 외부 AI 호출, 비용 사용은 없습니다. 작업 저장은 AI 실행이나 완료를 뜻하지 않습니다.

## GitHub Pages

Vite base 경로는 저장소 경로인 `/WT-OFFICE/`로 설정되어 있습니다. `main`의 프런트엔드 변경은 GitHub Actions에서 빌드하고 Pages에 배포합니다. 배포 전에 저장소 **Settings → Pages → Build and deployment → Source**를 **GitHub Actions**로 설정해야 합니다. Pages에서는 백엔드나 로컬 SQLite를 실행하지 않습니다.

## 운영 규칙

- 작업 지침은 [AGENTS.md](AGENTS.md)를 따릅니다.
- `release note.md`는 사용자가 기록을 요청했을 때만 갱신합니다.
- 커밋, 푸시, 브랜치 변경, 저장소 설정 변경, 실제 배포는 사용자 요청이 있어야 합니다.
- 다음 Phase는 사용자 승인 후 진행합니다.
