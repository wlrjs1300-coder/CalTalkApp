# 테스트 전략

## 현재 기준

기준 커밋 `17ef45b`에서 Gradle 전체 테스트 결과는 다음과 같다.

| 항목 | 결과 |
|---|---:|
| 테스트 | 96 |
| 테스트 스위트 | 17 |
| 실패 | 0 |
| 오류 | 0 |
| 건너뜀 | 0 |

이 숫자는 해당 커밋의 현재 결과이며 후속 기능 추가 시 함께 갱신한다.

## 실제 PostgreSQL 검증

통합 테스트는 PostgreSQL 17 Testcontainers를 사용한다. H2를 사용하지 않는 이유는 다음 동작을 대체 DB가 아니라 실제 엔진에서 확인하기 위해서다.

- Flyway V1~V4 SQL
- timestamp with time zone
- 부분 유니크 인덱스
- `ON CONFLICT DO NOTHING RETURNING`
- FK, CHECK, CASCADE
- 비관적 잠금과 동시 트랜잭션
- Spring Session JDBC schema와 저장 형식

테스트 컨텍스트는 production migration을 그대로 적용하고 Hibernate mapping을 validate한다.

## 테스트 계층

### MockMvc 통합 테스트

Controller부터 Security filter, Service, Repository와 PostgreSQL까지 연결한다. HTTP status, JSON 필드, header와 DB 결과를 함께 검증한다.

주요 범위:

- 회원가입의 정규화·검증·중복·BCrypt 저장
- 로그인 성공·실패 일반화와 session ID 교체
- 로그아웃과 회원 탈퇴의 현재 세션 삭제
- 현재 사용자 조회와 시간대 변경
- 일정 생성·기간 조회·상세·수정·삭제
- 다른 사용자 자원 은닉
- optimistic locking과 변경 이력
- confirmation 발급·승인·만료·소비·대체
- strict JSON unknown field 거부와 민감정보 비반사

Security 단위 편의 기능을 쓰는 테스트도 있지만, 브라우저 CSRF 계약은 별도 실제 HTTP 테스트가 담당한다.

### 실제 HTTP 브라우저 CSRF 테스트

랜덤 포트로 실제 서버를 실행하고 Java HttpClient와 CookieManager를 사용한다. MockHttpSession을 직접 전달하거나 MockMvc의 CSRF token 주입만으로 대체하지 않는다.

검증 흐름:

1. `GET /api/v1/csrf`의 `XSRF-TOKEN` 발급과 cookie 속성
2. 로그인 후 HttpOnly `CALTALK_SESSION` 저장
3. 같은 cookie jar로 일정 POST/PATCH/DELETE
4. 사용자 PATCH
5. CSRF header 누락·불일치와 cookie 누락의 JSON `403`
6. 로그아웃 후 이전 session/token 재사용의 `401`

### JDBC Session 테스트

- Flyway V4의 공식 테이블·인덱스·FK 확인
- 로그인 후 `SPRING_SESSION` 행과 Security context 저장 확인
- 다음 요청에서 인증 상태 복원
- 로그아웃과 회원 탈퇴 후 현재 세션만 제거
- 다른 사용자의 세션 유지

### confirmation 동시성 테스트

`CountDownLatch`로 두 작업의 시작 시점을 맞추며 `Thread.sleep`에 의존하지 않는다.

- 최초 CREATE 후보가 동일 ID로 수렴
- stale CREATE snapshot을 SUPERSEDED 처리
- stale UPDATE target version을 SUPERSEDED 처리
- replacement PENDING 하나 유지
- 두 응답이 최신 confirmation ID로 수렴
- `500`과 일정·이력 부분 커밋 없음

## 트랜잭션과 rollback 검증

테스트는 HTTP 응답만 확인하지 않고 PostgreSQL 행을 직접 조회한다. 충돌 시 일정과 이력이 저장되지 않는지, 승인 시 일정과 이력이 함께 저장되는지, 삭제 CASCADE와 다른 사용자 데이터 보존이 유지되는지 확인한다.

회원 탈퇴 DB 실패 시 rollback과 세션 유지, 성공 시 DB commit 이후 현재 session 제거도 검증 범위에 포함한다.

## 실행 방법

Docker를 실행할 수 있는 환경에서 backend 디렉터리 기준으로 실행한다.

```powershell
.\gradlew.bat clean test --console=plain
.\gradlew.bat compileTestJava --warning-mode all --console=plain
```

두 번째 명령은 테스트 코드의 compiler 및 Gradle deprecated 경고를 확인하기 위한 별도 검증이다.

## 회귀 테스트 원칙

- 실패 테스트를 삭제하거나 비활성화하지 않는다.
- 기대 HTTP status와 DB 검증을 완화해 통과시키지 않는다.
- PostgreSQL 전용 동작은 실제 PostgreSQL에서 검증한다.
- 인증 상태는 JDBC Session 복원을 통해 확인한다.
- 동시성 테스트는 결과 ID, 상태, 개수와 부분 커밋 여부를 함께 검증한다.
- API 계약 변경 시 구현, 테스트와 문서의 수치를 같은 변경에서 갱신한다.

## 아직 없는 테스트 범위

React frontend가 아직 없으므로 Playwright 등의 실제 브라우저 UI E2E는 구현하지 않았다. 현재 실제 HTTP CSRF 테스트는 브라우저의 cookie/header 동작을 서버 관점에서 재현하지만 렌더링, 화면 이동과 사용자 상호작용은 다루지 않는다. 운영 proxy, HTTPS와 Secure cookie도 배포 단계에서 별도 검증해야 한다.
