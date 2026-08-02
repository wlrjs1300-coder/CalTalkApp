# 개발 로드맵

## 1. 현재 완료 범위

Stage 10에서 다음 백엔드 범위를 완료했다.

- 이메일 회원가입과 세션 로그인·로그아웃
- 사용자 조회, 시간대 변경, 회원 탈퇴
- 일정 CRUD, 소유권 은닉, optimistic locking과 변경 이력
- CREATE_EVENT·UPDATE_EVENT 충돌 confirmation과 승인
- stale snapshot·target version 대체와 동시 요청 수렴
- Spring Session JDBC, cookie CSRF, 제한된 CORS와 strict JSON
- PostgreSQL Testcontainers 기반 96개 테스트

## 2. 포트폴리오 공개 범위

현재 공개 범위는 백엔드 설계와 구현, DB migration, 보안 계약, 테스트와 동시성 처리다. frontend, 자연어 해석, 카카오 연결과 운영 배포는 완료 범위에 포함하지 않는다.

백엔드 공개 기준:

- 전체 회귀 테스트 성공
- 실제 API와 문서 계약 일치
- 비밀값과 로컬 파일 미추적
- 구현 범위와 예정 범위 구분
- 동시성 선택과 트랜잭션 경계를 설명할 수 있음

## 3. React frontend

예정 기능:

- 회원가입과 로그인 화면
- CSRF bootstrap과 credentials 공통 client
- 현재 사용자·시간대 설정
- 일정 목록, 상세, 생성, 수정과 삭제
- 충돌 목록과 confirmation 승인 UI
- 로그아웃과 회원 탈퇴
- 공통 오류 및 인증 만료 처리

완료 기준은 주요 API 흐름을 화면에서 수행하고, 새로고침 이후에도 JDBC Session 인증이 복원되는 것이다.

## 4. 브라우저 E2E

React 구현 이후 실제 브라우저 자동화를 추가한다.

- 로그인 → 일정 생성 → 수정 → 삭제 → 로그아웃
- `XSRF-TOKEN` cookie와 `X-XSRF-TOKEN` header
- confirmation 충돌 표시와 승인
- 로그아웃 후 보호 화면 접근 차단
- 세션 만료와 재로그인
- 다른 origin과 CSRF 실패 화면 처리

현재 Java HttpClient 통합 테스트는 서버의 브라우저 계약을 검증하지만 UI E2E를 대신하지 않는다.

## 5. 운영 배포

예정 작업:

- HTTPS와 `SESSION_COOKIE_SECURE=true`
- 실제 frontend origin 또는 동일 출처 reverse proxy
- 외부 PostgreSQL과 migration 실행 정책
- 환경 변수 및 credential 외부 주입
- 컨테이너 health/readiness와 무중단 종료 검증
- 배포 URL과 운영 점검 절차 문서화

운영 준비 완료 판단은 개발 CORS와 로컬 credential에 의존하지 않고, backup·관측·복구 절차까지 검증한 뒤 내린다.

## 6. 자연어 일정 처리

자연어 입력은 아직 구현하지 않았다. 후속 단계에서는 OpenAI 기반 해석 결과를 바로 저장하지 않고 structured output으로 검증 가능한 일정 후보를 만든다.

- 날짜·시간·timezone 해석
- 제목과 선택적 장소 추출
- 불충분한 입력의 재질문
- 명시적 사용자 확인
- 기존 confirmation 충돌 모델과 연결
- 원문과 모델 결과의 보존 범위 최소화

## 7. pending command와 idempotency

자연어와 외부 채널은 네트워크 재전송과 중복 요청을 고려해야 한다. 현재 DB에는 pending command와 idempotency record가 없다.

예정 설계:

- 해석 중인 명령과 confirmation을 분리
- 재질문 만료와 confirmation 5분 만료 구분
- 채널별 idempotency key
- 같은 외부 event의 중복 저장 방지
- 처리 결과와 재시도 가능 여부 기록
- 회원 탈퇴 시 신규 사용자 연관 테이블 삭제 범위 갱신

## 8. Kakao 연동

PoC 문서는 참고 자료이며 현재 정식 연동은 없다.

- 일회성 link code 발급
- 연결 상태 조회와 연결 해제
- Kakao user와 CalTalk user의 최소 정보 연결
- webhook 또는 polling 구조 재검토
- 요청 인증, 재전송과 rate limit 계약 확인
- 연결 해제 시 pending 상태 정리

공식 플랫폼 계약을 확인한 뒤 API와 보안 모델을 확정한다.

## 9. 운영 안정화

- 로그인·회원가입·외부 webhook rate limiting
- 구조화된 application log와 개인정보 제거
- health, metric, tracing과 alerting
- PostgreSQL backup, restore drill과 보존 정책
- migration 배포 전후 검증
- dependency와 container 취약점 점검
- CI에서 compile, test와 문서 링크 검사
- 장애 대응 및 rollback 절차

Redis는 실제 rate limiting, cache 또는 비동기 조정 요구가 확정될 때 사용 범위를 결정한다.

## 10. 완료 기준

### Web MVP

- React에서 인증, 일정 CRUD와 confirmation 승인 가능
- 브라우저 E2E 통과
- 접근성·오류·loading 상태 처리
- README에 실제 화면과 실행 흐름 제공

### 자연어 일정 MVP

- structured output schema 검증
- 불확실한 입력은 저장 전 재질문
- 중복 요청 방지
- 충돌 confirmation과 동일한 승인 규칙 적용

### 외부 채널 확장

- 연결·해제와 사용자 매핑 보안 검증
- 외부 요청 인증과 재전송 계약 검증
- 채널 장애가 Web 일정 기능의 트랜잭션에 영향을 주지 않음

### 운영 단계

- HTTPS, Secure cookie와 운영 CORS 검증
- CI/CD, monitoring, alerting과 backup 복구 검증
- 비밀값 외부 주입과 최소 권한 점검
- 실제 배포 URL 및 운영 제한사항 공개
