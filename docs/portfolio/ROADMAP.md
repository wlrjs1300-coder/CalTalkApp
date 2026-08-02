# 개발 로드맵

## 현재 상태

**Local Web MVP Core Complete · Deployment Configuration Complete · External Deployment Pending**

기준 커밋 `fdf4c02`에서 backend 96개, frontend 30개, 개발 Playwright E2E 5개와 production Compose E2E 5개가 통과합니다. 현재 공개 범위는 로컬 Web MVP의 backend, React frontend, PostgreSQL migration, 보안 계약, 동시성 처리, 실제 브라우저 검증과 범용 Docker 배포 구성입니다.

## 완료된 범위

### Backend core

- 회원가입, 로그인, 로그아웃과 Spring Session JDBC
- 현재 사용자 조회, time zone 변경, 회원 탈퇴
- 일정 CRUD, 소유권 은닉, optimistic locking과 변경 이력
- CREATE_EVENT·UPDATE_EVENT 충돌 confirmation
- stale snapshot·target version 교체와 동시 요청 수렴
- cookie CSRF, 제한된 CORS, strict JSON
- PostgreSQL 17 Testcontainers 기반 통합 테스트

### React frontend

- Vite·React·TypeScript foundation
- React Router 공개·보호 경로
- 회원가입, 로그인, 로그아웃과 session restore
- 현재 사용자와 time zone UI
- 일정 목록·상세·생성·수정·삭제
- location KEEP·SET·REMOVE
- conflict dialog와 confirmation 승인
- stale replacement confirmation 교체
- loading·empty·error와 접근성 기본 구조

### Browser E2E

- 실제 PostgreSQL·Flyway·Spring Boot·Vite·Chromium orchestration
- 인증, session cookie, CSRF cookie/header
- 일정 CRUD와 UTC 불변
- CREATE_EVENT·UPDATE_EVENT confirmation
- 로그아웃 후 이전 session 재사용 차단
- 고유 테스트 데이터와 실제 회원 탈퇴 cleanup

## 완료된 배포 준비와 다음 단계 1: 외부 운영 배포

- same-origin Nginx reverse proxy와 production container 구성
- `SESSION_COOKIE_SECURE=true`, 운영 CORS 환경변수와 forwarded header 지원
- PostgreSQL credential 외부 주입, named volume과 private network
- Flyway V1~V4, health/readiness와 graceful shutdown 검증
- production Compose 기반 Playwright E2E 5개 통과
- 실제 HTTPS edge, DNS와 secrets manager 구성
- 실제 배포 URL과 운영 제한사항 공개

배포 구성과 로컬 production smoke는 완료했지만 실제 외부 배포는 수행하지 않았으므로 production ready 또는 공개 서비스 완료로 표현하지 않습니다.

## 다음 단계 2: 운영 안전성

- CI/CD
- monitoring, metric, tracing과 alerting
- PostgreSQL backup, restore drill과 보존 정책
- dependency와 container 취약점 점검
- rate limiting과 로그인 시도 제한
- 부하·장시간 session cleanup 검증

## 다음 단계 3: 자연어 일정 입력

- OpenAI structured output schema
- 날짜·시간·time zone·제목·장소 추출
- 불명확한 입력에 대한 사용자 재질문
- 기존 confirmation 모델과 연결
- prompt와 model output의 개인정보 최소화

자연어 일정 입력과 OpenAI 연동은 현재 구현 범위가 아닙니다.

## 다음 단계 4: pending command와 idempotency

- 해석 중인 명령과 confirmation 분리
- channel별 idempotency key
- 외부 요청 재전송과 중복 저장 방지
- 재시도 가능 결과 기록
- 회원 탈퇴 시 신규 사용자 연계 데이터 삭제 범위 확장

## 다음 단계 5: 카카오 연동

- CalTalk 사용자와 카카오 사용자 연결
- link code 발급·조회·해제
- webhook 또는 polling 구조 결정
- 외부 요청 인증·재전송·rate limit 계약
- 연결 해제와 pending 상태 정리

카카오 연동은 현재 구현되어 있지 않습니다.

## 다음 단계 6: 일정 기능 확장

- confirmation 취소 API
- 반복 일정
- 알림과 reminder
- 사용자 선택 가능한 DST overlap 정책 검토

## 단계별 완료 기준

### Local Web MVP

- React 인증·일정 CRUD·confirmation 가능
- frontend와 backend 테스트 통과
- 실제 browser E2E 통과
- README에 실행법과 제한사항 공개

현재 이 단계는 완료했습니다.

### Deployment

- HTTPS, Secure cookie, 운영 CORS 검증
- CI/CD와 rollback 절차
- monitoring, alerting, backup/recovery 검증
- 실제 배포 URL 공개

### AI schedule assistant

- structured output 검증
- 불명확한 입력 재질문
- idempotency와 confirmation 연결
- 개인정보와 prompt 보존 정책 확정

### External channel

- 카카오 계정 연결 보안 검증
- 외부 요청 인증과 재전송 계약
- Web과 외부 channel 사이 transaction 일관성 검증
