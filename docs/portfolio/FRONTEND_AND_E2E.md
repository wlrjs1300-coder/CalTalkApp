# Frontend와 Browser E2E

## Frontend 구조

Frontend는 React 19, TypeScript 5.9와 Vite 8 기반 SPA입니다. React Router 8.3.0의 declarative routing을 사용하고, 서버 상태는 TanStack Query, form과 입력 검증은 React Hook Form과 Zod가 담당합니다.

```text
src/
  api/                 HTTP, CSRF, 오류 계약
  app/                 provider와 router
  features/auth/       현재 사용자 query와 route guard
  features/schedule/   query, mutation, form, dialog
  features/user/       time zone 변경
  pages/               signup, login, home
  test/                Vitest 사용자 흐름
```

## 인증 흐름

Frontend는 `GET /api/v1/users/me`를 인증 상태의 source of truth로 사용합니다. session ID를 JavaScript storage에 저장하지 않으며 `CALTALK_SESSION` 값을 직접 읽지 않습니다.

1. 로그인 요청을 `credentials: include`로 전송합니다.
2. 보호 경로 진입 시 현재 사용자 query를 조회합니다.
3. 새로고침 뒤에도 서버의 Spring Session JDBC에서 인증을 복원합니다.
4. 명시적인 `401`은 비인증으로 처리하고 로그인 화면으로 이동합니다.
5. network error는 인증 실패로 단정하지 않고 별도의 오류 상태로 처리합니다.

## CSRF client

상태 변경 요청 전에 API client가 `GET /api/v1/csrf`를 호출합니다. 브라우저가 받은 `XSRF-TOKEN` cookie를 읽어 `X-XSRF-TOKEN` header로 전달하고 모든 API 요청에 credentials를 포함합니다.

로그인과 회원가입은 backend 정책에 따라 CSRF 예외이며, 사용자 변경, 일정 생성·수정·삭제, confirmation 승인과 로그아웃은 CSRF가 필요합니다. `403`이 발생하면 token을 한 번 갱신한 뒤 요청을 재시도합니다.

## 일정과 time zone

- 일정 입력과 표시는 사용자 IANA time zone 기준입니다.
- API와 DB 값은 UTC ISO instant입니다.
- time zone을 변경하면 query를 갱신해 표시만 다시 계산합니다.
- DST gap은 입력 오류로 거부합니다.
- DST overlap은 더 이른 instant를 선택합니다.
- 수정과 삭제에는 backend가 반환한 최신 `version`을 사용합니다.
- location 유지 시 필드를 생략하고, 설정은 문자열, 제거는 `null`을 전달합니다.

## confirmation UI

일정 생성·수정에서 `409 SCHEDULE_CONFLICT`를 받으면 form을 닫고 충돌 요약과 confirmation dialog를 표시합니다. 승인 성공 후 일정 목록과 상세 query를 갱신합니다.

승인 직전에 snapshot이나 target version이 바뀌어 `CONFIRMATION_SUPERSEDED`가 반환되면 응답의 최신 `confirmationId`와 충돌 목록으로 dialog 상태를 교체합니다. 자동 교체는 최대 3회로 제한하며 사용자가 최신 내용을 다시 확인해야 합니다.

## 접근성과 상태 처리

- form control과 label 연결
- dialog의 `role=dialog`, modal과 제목 연결
- 오류의 `role=alert`
- 성공 알림의 `role=status`
- loading, empty, network error와 retry UI
- Escape를 통한 dialog 닫기와 이전 focus 복원

## Playwright 구성

Playwright E2E는 단일 Chromium worker로 안정적으로 실행합니다.

- base URL: `http://localhost:5173`
- 로컬 retry: 0
- trace: first retry
- screenshot: failure only
- video: retain on failure
- 실제 backend와 frontend는 Playwright `webServer`가 관리
- PostgreSQL과 Redis는 Node orchestration이 Docker Compose로 관리

기본 로컬 5432·6379와의 충돌을 피하기 위해 E2E는 PostgreSQL 55432와 Redis 56379를 사용합니다. Spring Boot에는 환경 변수로 E2E JDBC URL을 전달하며 운영 설정 파일은 수정하지 않습니다.

Docker healthcheck와 HTTP readiness를 사용하고 고정 sleep은 사용하지 않습니다. 실행 전부터 동작하던 Compose service는 보존하고, 이번 실행에서 시작한 service만 종료합니다.

## E2E 시나리오

현재 5개 테스트가 다음 계약을 검증합니다.

- 회원가입, 중복 이메일, 정상·실패 로그인
- 새로고침 후 session restore와 `CALTALK_SESSION` 속성
- frontend가 전송한 CSRF header와 요청 origin
- CSRF header 누락 요청의 `403` JSON
- 일정 CRUD와 location KEEP·SET·REMOVE
- 사용자 time zone 변경과 API UTC 값 불변
- CREATE_EVENT와 UPDATE_EVENT confirmation 승인
- 로그아웃 후 이전 session 재사용 `401`

API route interception이나 mock server를 사용하지 않으며 DB에 직접 테스트 데이터를 삽입하지 않습니다. 테스트마다 고유 이메일과 일정 제목을 생성하고 실제 회원 탈퇴 API로 정리합니다.

replacement confirmation의 결정적인 동시 경쟁은 backend PostgreSQL 통합 테스트가 담당합니다. Browser E2E는 사용자가 확인하고 승인하는 흐름에 집중합니다.

## 오류 측정 표현

공통 fixture는 처리되지 않은 브라우저 예외인 `pageerror`를 수집하며 현재 결과는 0입니다. 의도적으로 검증하는 401·403·409 응답은 브라우저 resource 메시지가 될 수 있으므로 console 메시지 전체가 없었다고 주장하지 않습니다.

## 실행

```powershell
cd frontend
npm run e2e:install
npm run e2e
```

- `npm run e2e:headed`
- `npm run e2e:ui`
- `npm run e2e:full`

실패 시 생성될 수 있는 trace, screenshot, video, `test-results`, `playwright-report`와 `blob-report`는 Git에서 제외됩니다.
